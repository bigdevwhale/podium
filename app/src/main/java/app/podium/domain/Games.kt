package app.podium.domain

import app.podium.data.Event
import app.podium.data.Ring
import app.podium.data.Run
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** One Games: [number] from 1, the days [from] until [to] (exclusive). */
data class Games(val number: Int, val from: LocalDate, val to: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(from) && d.isBefore(to)
    val days: Int get() = (to.toEpochDay() - from.toEpochDay()).toInt()
}

data class Tally(val gold: Int = 0, val silver: Int = 0, val bronze: Int = 0) {
    val total: Int get() = gold + silver + bronze
    operator fun plus(m: Medal) = when (m) {
        Medal.GOLD -> copy(gold = gold + 1)
        Medal.SILVER -> copy(silver = silver + 1)
        Medal.BRONZE -> copy(bronze = bronze + 1)
    }
    operator fun plus(o: Tally) = Tally(gold + o.gold, silver + o.silver, bronze + o.bronze)

    /** Ordered the way medal tables are: gold first, then silver, then bronze. */
    val rank: Long get() = gold * 1_000_000L + silver * 1_000L + bronze
}

/** A heat and what it won, for tables and protocols. */
data class Placed(val run: Run, val event: Event, val award: Award)

object GamesCalendar {
    fun monday(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun date(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** The Games [d] falls in; numbers below 1 are the days before the first Games opened. */
    fun of(d: LocalDate, from: Long, weeks: Int): Games {
        val len = 7L * weeks.coerceAtLeast(1)
        val i = Math.floorDiv(d.toEpochDay() - from, len)
        val start = LocalDate.ofEpochDay(from + i * len)
        return Games((i + 1).toInt(), start, start.plusDays(len))
    }

    fun number(n: Int, from: Long, weeks: Int): Games = of(LocalDate.ofEpochDay(from + (n - 1L) * 7 * weeks.coerceAtLeast(1)), from, weeks)

    /** Every heat with what it won, newest first. */
    fun placed(events: List<Event>, runs: List<Run>): List<Placed> {
        val byEvent = runs.groupBy { it.eventId }
        val ev = events.associateBy { it.id }
        return runs.mapNotNull { r -> ev[r.eventId]?.let { e -> Placed(r, e, Results.award(e, r, byEvent[e.id].orEmpty())) } }
            .sortedByDescending { it.run.start }
    }

    /** The medal table of one Games, by part of life. */
    fun table(placed: List<Placed>, g: Games, zone: ZoneId = ZoneId.systemDefault()): Map<Ring, Tally> =
        placed.filter { date(it.run.start, zone) in g && it.award.medal != null }
            .groupBy { it.event.ringOf }
            .mapValues { (_, l) -> l.fold(Tally()) { t, p -> t + p.award.medal!! } }

    fun tally(placed: List<Placed>): Tally = placed.mapNotNull { it.award.medal }.fold(Tally()) { t, m -> t + m }

    /** The best heat of each discipline within a Games, when it was raced there at least twice. */
    fun gamesRecords(placed: List<Placed>, g: Games, zone: ZoneId = ZoneId.systemDefault()): Map<Long, Run> =
        placed.filter { date(it.run.start, zone) in g }.groupBy { it.event }
            .filterValues { l -> l.count { it.run.counts } >= 2 }
            .mapNotNull { (e, l) -> Results.record(e, l.map { it.run })?.let { e.id to it } }.toMap()

    /**
     * The flame: days in a row with at least one heat, counted back from today (or from yesterday,
     * so it doesn't go out in the morning before the first heat).
     */
    fun flame(runs: List<Run>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = runs.map { date(it.start, zone) }.toHashSet()
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }
}
