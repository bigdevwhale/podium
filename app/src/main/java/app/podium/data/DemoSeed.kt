package app.podium.data

import android.content.Context
import app.podium.AppContainer
import app.podium.R
import app.podium.domain.GamesCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * A made-up season for the demo build (`-Pdemo`): nine disciplines raced over twelve weeks, getting
 * faster the way people do, with tired days, rushed ones, pit stops for the phone, and a relay.
 */
object DemoSeed {
    private class Plan(
        val starter: Int,
        val from: Double,
        val to: Double,
        /** Heats a week, roughly. */
        val often: Double,
        val at: Int,
        val weights: List<Double> = emptyList(),
        val days: Set<DayOfWeek>? = null,
    )

    suspend fun seedIfEmpty(context: Context, container: AppContainer) {
        val dao = container.db.dao()
        if (dao.events().isNotEmpty()) return
        val rnd = Random(7)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weeks = 12
        val gamesFrom = GamesCalendar.monday(today).minusWeeks(weeks - 2L)
        val min = 60_000.0
        val plans = listOf(
            Plan(0, 34 * min, 25 * min, 5.5, 7 * 60 + 5, listOf(0.12, 0.3, 0.2, 0.38)),
            Plan(1, 19 * min, 13.5 * min, 4.0, 20 * 60 + 25, listOf(0.5, 0.25, 0.25)),
            Plan(2, 31 * min, 22 * min, 1.8, 21 * 60, listOf(0.35, 0.3, 0.35)),
            Plan(3, 23 * min, 15 * min, 3.0, 9 * 60 + 40, days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
            Plan(4, 41 * min, 33 * min, 1.0, 11 * 60, days = setOf(DayOfWeek.SATURDAY)),
            Plan(5, 55_000.0, 152_000.0, 4.0, 7 * 60 + 50),
            Plan(6, 22 * min, 46 * min, 3.0, 22 * 60 + 10),
            Plan(8, 20 * min, 20 * min, 2.0, 13 * 60 + 5, days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            Plan(9, 31.5 * min, 27.2 * min, 2.0, 18 * 60 + 40, days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)),
        )
        val ids = plans.mapIndexed { i, p -> dao.insert(Starters.event(context, Starters.all[p.starter]).copy(order = i, createdAt = gamesFrom.atStartOfDay(zone).toInstant().toEpochMilli())) }
        val relay = dao.insert(Relay(name = context.getString(R.string.demo_relay), legs = "${ids[1]},${ids[2]}"))

        val start = gamesFrom.minusDays(3)
        val total = (today.toEpochDay() - start.toEpochDay()).toInt().coerceAtLeast(1)

        /** One heat of plan [i] at [at]; returns how long it took on the clock, pits included. */
        suspend fun heat(i: Int, at: Long, progress: Double, relayKey: Long = 0): Long {
            val p = plans[i]
            val e = dao.event(ids[i])!!
            val conditions = mutableSetOf<Condition>()
            var k = 1.0
            when (rnd.nextInt(100)) {
                in 0..13 -> { conditions += Condition.TIRED; k *= 1.13 }
                in 14..21 -> { conditions += Condition.INTERRUPTED; k *= 1.17 }
                in 22..35 -> { conditions += Condition.FRESH; k *= 0.95 }
                in 36..44 -> { conditions += Condition.COFFEE; k *= 0.97 }
            }
            val form = when (rnd.nextInt(100)) {
                in 0..5 -> Form.RUSHED.also { k *= 0.84 }
                in 6..25 -> Form.OK
                else -> Form.CLEAN
            }
            // People improve fast at first, then slower: an easing curve towards the target.
            val curve = 1 - (1 - progress) * (1 - progress)
            val base = p.from + (p.to - p.from) * curve
            val noise = 1 + (rnd.nextDouble() - 0.5) * 0.14
            var ms = when (e.sportOf) {
                Sport.MARATHON -> base * noise / (if (Condition.TIRED in conditions) 1.1 else 1.0)
                Sport.PACE -> e.target * (1 + (rnd.nextDouble() - 0.5) * 0.3 * (1.2 - curve))
                Sport.SPRINT -> base * noise * k
            }.toLong()
            ms -= ms % 100
            val splits = if (p.weights.isNotEmpty()) {
                val w = p.weights.map { it * (1 + (rnd.nextDouble() - 0.5) * 0.25) }
                val sum = w.sum()
                w.runningReduce(Double::plus).map { (it / sum * ms).toLong() }.toMutableList().also { it[it.lastIndex] = ms }.joinToString(",")
            } else ""
            val guess = if (e.sportOf == Sport.SPRINT && relayKey == 0L && rnd.nextInt(100) < 45) {
                val bias = when (e.ringOf) {
                    Ring.HOME -> 0.24
                    Ring.WORK -> 0.12
                    else -> 0.07
                } * (1.1 - curve * 0.6)
                (ms / (1 + bias + (rnd.nextDouble() - 0.5) * 0.1) / 30_000).toLong() * 30_000
            } else 0L
            val pit = if (e.sportOf == Sport.SPRINT && rnd.nextInt(100) < 11) listOf("PHONE", "PHONE", "PEOPLE", "BREAK")[rnd.nextInt(4)] to (40_000L + rnd.nextLong(150_000)) else null
            dao.insert(
                Run(
                    eventId = e.id, start = at, duration = ms, splits = splits,
                    pitMs = pit?.second ?: 0, pits = pit?.let { "${it.first}=${it.second}" } ?: "",
                    guess = guess, form = form.name, conditions = Run.conditionsOf(conditions),
                    relay = relayKey, relayId = if (relayKey > 0) relay else 0,
                )
            )
            return ms + (pit?.second ?: 0)
        }

        var day = start
        var n = 0
        while (!day.isAfter(today)) {
            val progress = n.toDouble() / total
            // A day off now and then (never the last few): the flame has gone out and been lit again.
            if (rnd.nextInt(100) < 9 && today.toEpochDay() - day.toEpochDay() > 6) {
                day = day.plusDays(1)
                n++
                continue
            }
            plans.forEachIndexed { i, p ->
                if (p.days != null && day.dayOfWeek !in p.days) return@forEachIndexed
                val chance = if (p.days != null) p.often / p.days.size else p.often / 7
                if (rnd.nextDouble() > chance) return@forEachIndexed
                // Today: only what's usually done by the morning.
                if (day == today && p.at > 10 * 60) return@forEachIndexed
                val minute = p.at + rnd.nextInt(-25, 26)
                val at = day.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
                // The kitchen often runs straight on into the tidy-up: the evening relay.
                if (i == 1 && rnd.nextInt(100) < 50) {
                    val first = heat(1, at, progress, relayKey = at)
                    heat(2, at + first + 1000, progress, relayKey = at)
                } else heat(i, at, progress)
            }
            day = day.plusDays(1)
            n++
        }
        container.settings.update {
            val g = GamesCalendar.of(today, gamesFrom.toEpochDay(), 4)
            it.copy(onboarded = true, gamesFrom = gamesFrom.toEpochDay(), gamesWeeks = 4, closedSeen = g.number - 2)
        }
    }
}
