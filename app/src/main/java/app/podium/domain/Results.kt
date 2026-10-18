package app.podium.domain

import app.podium.data.Event
import app.podium.data.GhostMode
import app.podium.data.Run
import app.podium.data.Sport
import kotlin.math.abs

enum class Medal { GOLD, SILVER, BRONZE }

/**
 * What a heat won when it was run: its place among every heat of the discipline up to then, a
 * medal for the top three once qualifying is over, and whether it was a new record.
 */
data class Award(val place: Int, val of: Int, val medal: Medal?, val record: Boolean, val qualifying: Boolean, val debut: Boolean)

/** The runner in the next lane: a past heat, or a made-up one (the standard, the pacemaker). */
data class Ghost(val mode: GhostMode, val total: Long, val splits: List<Long>, val run: Run?, val pacer: Boolean = false)

/**
 * The scoreboard at one moment of a heat. Positions are 0..1 round the track (yours can run past 1
 * in a marathon: extra laps). [lastSplit] is how the latest split compared with the ghost's (minus
 * is earlier). [mark] is the ghost's next time to beat, and [toMark] how far off it still is (minus
 * once it has passed).
 */
data class Board(
    val ghostAt: Float,
    val youAt: Float,
    val lastSplit: Long?,
    val mark: Long,
    val toMark: Long,
    val finishMark: Boolean,
)

object Results {
    /** Heats before medals are given: a standard has to exist first. */
    const val QUALIFYING = 3
    private const val DAY = 86_400_000L

    /** Lower is better, whatever the sport. */
    fun score(e: Event, duration: Long): Long = when (e.sportOf) {
        Sport.SPRINT -> duration
        Sport.MARATHON -> -duration
        Sport.PACE -> abs(duration - e.target)
    }

    fun valid(runs: List<Run>): List<Run> = runs.filter { it.counts }

    /** Best first; on a tie, the earlier heat keeps the place. */
    fun ranked(e: Event, runs: List<Run>): List<Run> = valid(runs).sortedWith(compareBy<Run>({ score(e, it.duration) }, { it.start }))

    fun record(e: Event, runs: List<Run>): Run? = ranked(e, runs).firstOrNull()

    fun podium(e: Event, runs: List<Run>): List<Run> = ranked(e, runs).take(3)

    fun median(xs: List<Long>): Long {
        if (xs.isEmpty()) return 0
        val s = xs.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }

    /** The qualifying standard: the median heat, or the target for a pace. */
    fun standard(e: Event, runs: List<Run>): Long? = when {
        e.sportOf == Sport.PACE -> e.target
        else -> valid(runs).map { it.duration }.takeIf { it.isNotEmpty() }?.let(::median)
    }

    /** The award [run] got among the discipline's heats [runs] (all of them; later ones are ignored). */
    fun award(e: Event, run: Run, runs: List<Run>): Award {
        val before = valid(runs).filter { it.start < run.start && it.id != run.id }
        val field = before + run
        val place = if (!run.counts) 0 else ranked(e, field).indexOfFirst { it.id == run.id && it.start == run.start } + 1
        val qualifying = before.size < QUALIFYING
        val medal = if (qualifying || place == 0) null else when (place) {
            1 -> Medal.GOLD
            2 -> Medal.SILVER
            3 -> Medal.BRONZE
            else -> null
        }
        return Award(place, field.size, medal, record = run.counts && before.isNotEmpty() && place == 1, qualifying = qualifying, debut = before.isEmpty())
    }

    /** Heats with a time for every section of the discipline as it is now. */
    fun split(e: Event, runs: List<Run>): List<Run> {
        val n = e.sections.size
        return if (n < 2) emptyList() else valid(runs).filter { it.splitTimes.size == n }
    }

    /** The best time of each section ever, ms; empty without sections. */
    fun bestSections(e: Event, runs: List<Run>): List<Long> {
        val full = split(e, runs)
        if (full.isEmpty()) return emptyList()
        return (0 until e.sections.size).map { i ->
            val xs = full.map { it.sections[i] }
            if (e.sportOf == Sport.MARATHON) xs.max() else xs.min()
        }
    }

    /** The perfect heat: every section at its best, put together. */
    fun sumOfBest(e: Event, runs: List<Run>): Long? = bestSections(e, runs).takeIf { it.isNotEmpty() && e.sportOf == Sport.SPRINT }?.sum()

    /** Which sections of [run] were the best ever when it was run. */
    fun goldSections(e: Event, run: Run, runs: List<Run>): List<Boolean> {
        val n = e.sections.size
        val mine = run.sections
        if (n < 2 || mine.size != n) return emptyList()
        val before = split(e, runs).filter { it.start < run.start && it.id != run.id }
        if (before.isEmpty()) return List(n) { false }
        return (0 until n).map { i -> before.all { mine[i] < it.sections[i] } }
    }

    fun ghost(e: Event, runs: List<Run>, mode: GhostMode, now: Long): Ghost? {
        if (e.sportOf == Sport.PACE && e.target > 0) {
            val n = e.sections.size
            val splits = if (n >= 2) (1..n).map { e.target * it / n } else emptyList()
            return Ghost(mode, e.target, splits, null, pacer = true)
        }
        val v = valid(runs).filter { it.start < now }
        if (v.isEmpty()) return null
        val n = e.sections.size
        fun of(r: Run) = Ghost(mode, r.duration, if (n >= 2 && r.splitTimes.size == n) r.splitTimes else emptyList(), r)
        return when (mode) {
            GhostMode.RECORD -> record(e, v)?.let(::of)
            GhostMode.LAST -> of(v.maxBy { it.start })
            GhostMode.MONTH -> of(v.filter { it.start <= now - 30 * DAY }.maxByOrNull { it.start } ?: v.minBy { it.start })
            GhostMode.STANDARD -> {
                val total = median(v.map { it.duration })
                val full = split(e, v)
                val splits = if (full.isEmpty()) emptyList() else {
                    val secs = (0 until n).map { i -> median(full.map { it.sections[i] }) }
                    val sum = secs.sum().coerceAtLeast(1)
                    secs.runningReduce(Long::plus).map { it * total / sum }
                }
                Ghost(mode, total, splits, null)
            }
        }
    }

    /** Where the ghost is after [t] ms: by its own splits when it has them, at its average pace otherwise. */
    fun ghostAt(g: Ghost, n: Int, t: Long): Float {
        if (g.total <= 0) return 0f
        if (t >= g.total) return 1f
        if (g.splits.size == n && n >= 2) {
            val i = g.splits.indexOfFirst { t < it }
            val from = g.splits.getOrNull(i - 1) ?: 0L
            val len = (g.splits[i] - from).coerceAtLeast(1)
            return (i + (t - from).toFloat() / len) / n
        }
        return t.toFloat() / g.total
    }

    /**
     * The scoreboard after [t] ms with [taken] splits behind. Between splits you are drawn running
     * the section at the ghost's pace, waiting at the line if it takes you longer.
     */
    fun board(e: Event, g: Ghost?, taken: List<Long>, t: Long): Board {
        val n = e.sections.size.coerceAtLeast(1)
        val k = taken.size.coerceAtMost(n - 1)
        if (g == null || g.total <= 0) {
            return Board(0f, if (n > 1) k.toFloat() / n else 0f, null, 0, 0, true)
        }
        val ghostSplits = g.splits.size == n && n >= 2
        val from = taken.lastOrNull() ?: 0L
        val sectionLen = if (ghostSplits) g.splits[k] - (g.splits.getOrNull(k - 1) ?: 0L) else g.total / n
        val within = ((t - from).toFloat() / sectionLen.coerceAtLeast(1)).coerceAtLeast(0f)
        val you = if (e.sportOf == Sport.MARATHON && n == 1) t.toFloat() / g.total else (k + within.coerceAtMost(0.985f)) / n
        val last = if (ghostSplits && taken.isNotEmpty()) taken.last() - g.splits[taken.size - 1] else null
        val mark = if (ghostSplits && e.sportOf == Sport.SPRINT) g.splits[k] else g.total
        return Board(ghostAt(g, n, t), you, last, mark, mark - t, finishMark = mark == g.total)
    }

    /** How a finished time stands against the ghost: minus is better, whatever the sport. */
    fun versus(e: Event, g: Ghost, duration: Long): Long = score(e, duration) - score(e, g.total)
}
