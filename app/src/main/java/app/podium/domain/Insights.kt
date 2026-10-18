package app.podium.domain

import app.podium.data.Condition
import app.podium.data.Event
import app.podium.data.Ring
import app.podium.data.Run
import app.podium.data.Sport
import kotlin.math.abs
import kotlin.math.sqrt

/** How a discipline has gone: the numbers on its page. */
data class Form(
    val heats: Int,
    val best: Long?,
    val median: Long?,
    /** Typical spread around the median, as a fraction. */
    val spread: Float?,
    /** The median of the last five against the first five, as a fraction; minus is better. */
    val trend: Float?,
    /** Heats since the record was set. */
    val sinceRecord: Int,
    val plateau: Boolean,
)

/** How good the entries before the start were in one part of life. */
data class Calls(val ring: Ring, val count: Int, val bias: Float, val close: Int)

/** How heats with a condition went against heats without, as a fraction of the time; plus is slower. */
data class Effect(val condition: Condition, val with: Int, val effect: Float)

object Insights {
    fun form(e: Event, runs: List<Run>): Form {
        val v = Results.valid(runs).sortedBy { it.start }
        val d = v.map { it.duration }
        val median = d.takeIf { it.isNotEmpty() }?.let(Results::median)
        val spread = if (d.size >= 3 && median != null && median > 0) {
            val mean = d.average()
            (sqrt(d.sumOf { (it - mean) * (it - mean) } / d.size) / median).toFloat()
        } else null
        val trend = if (d.size >= 8) {
            val first = Results.median(d.take(5)).toFloat()
            val last = Results.median(d.takeLast(5)).toFloat()
            val raw = (last - first) / first.coerceAtLeast(1f)
            when (e.sportOf) {
                Sport.SPRINT -> raw
                Sport.MARATHON -> -raw
                Sport.PACE -> {
                    val f = v.take(5).map { abs(it.duration - e.target) }
                    val l = v.takeLast(5).map { abs(it.duration - e.target) }
                    (Results.median(l) - Results.median(f)).toFloat() / e.target.coerceAtLeast(1)
                }
            }
        } else null
        val record = Results.record(e, v)
        val since = if (record == null) 0 else v.count { it.start > record.start }
        val recent = v.takeLast(6).map { it.duration }
        val recentSpread = if (recent.size == 6) (recent.max() - recent.min()).toFloat() / Results.median(recent).coerceAtLeast(1) else 1f
        val plateau = e.sportOf != Sport.PACE && v.size >= 10 && since >= 6 && recentSpread < 0.12f
        return Form(v.size, record?.duration, median, spread, trend, since, plateau)
    }

    /**
     * The entry before the start against the time it took, per part of life. [Calls.bias] is how much
     * longer things took than entered (0.4: forty per cent longer); [Calls.close] counts entries
     * within five per cent.
     */
    fun calls(events: List<Event>, runs: List<Run>): List<Calls> {
        val ring = events.associate { it.id to it.ringOf }
        return runs.filter { it.guess > 0 && it.duration > 0 }.groupBy { ring[it.eventId] ?: Ring.OTHER }
            .map { (r, l) ->
                val errs = l.map { (it.duration - it.guess).toFloat() / it.guess }
                Calls(r, l.size, errs.average().toFloat(), errs.count { abs(it) <= 0.05f })
            }
            .sortedByDescending { it.count }
    }

    /** All entries together: count, average bias and how many were within five per cent. */
    fun allCalls(runs: List<Run>): Calls? {
        val l = runs.filter { it.guess > 0 && it.duration > 0 }
        if (l.isEmpty()) return null
        val errs = l.map { (it.duration - it.guess).toFloat() / it.guess }
        return Calls(Ring.OTHER, l.size, errs.average().toFloat(), errs.count { abs(it) <= 0.05f })
    }

    /**
     * Each condition's effect on the time of sprints: every heat is measured against its
     * discipline's median, and heats with the condition are compared with heats without.
     */
    fun effects(events: List<Event>, runs: List<Run>): List<Effect> {
        val sprints = events.filter { it.sportOf == Sport.SPRINT }.associateBy { it.id }
        val v = Results.valid(runs).filter { it.eventId in sprints }
        val medians = v.groupBy { it.eventId }.mapValues { (_, l) -> Results.median(l.map { it.duration }).coerceAtLeast(1) }
        val rel = v.map { it to it.duration.toFloat() / medians.getValue(it.eventId) }
        return Condition.entries.mapNotNull { c ->
            val with = rel.filter { c in it.first.conditionSet }.map { it.second }
            val without = rel.filter { c !in it.first.conditionSet }.map { it.second }
            if (with.size < 3 || without.size < 3) null else Effect(c, with.size, with.average().toFloat() - without.average().toFloat())
        }.sortedByDescending { abs(it.effect) }
    }

    /** Pit stops by reason, total ms, the longest first. */
    fun pits(runs: List<Run>): List<Pair<String, Long>> =
        runs.flatMap { it.pitList }.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }.toList().sortedByDescending { it.second }
}
