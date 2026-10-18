package app.podium

import app.podium.data.Backup
import app.podium.data.Event
import app.podium.data.Everything
import app.podium.data.Form
import app.podium.data.GhostMode
import app.podium.data.Heat
import app.podium.data.Relay
import app.podium.data.Ring
import app.podium.data.Run
import app.podium.data.Sport
import app.podium.domain.GamesCalendar
import app.podium.domain.Insights
import app.podium.domain.Medal
import app.podium.domain.Relays
import app.podium.domain.Results
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PodiumTest {
    private val sprint = Event(id = 1, name = "Kitchen", sport = Sport.SPRINT.name, splits = "Dishes\nHob\nFloor")
    private val plank = Event(id = 2, name = "Plank", sport = Sport.MARATHON.name)
    private val lunch = Event(id = 3, name = "Lunch", sport = Sport.PACE.name, target = 1_200_000)
    private val day = 86_400_000L

    private fun run(id: Long, e: Event, d: Long, at: Long = id * day, form: Form = Form.CLEAN, splits: String = "") =
        Run(id = id, eventId = e.id, start = at, duration = d, form = form.name, splits = splits)

    @Test
    fun qualifyingFirstThenMedals() {
        val runs = listOf(run(1, sprint, 600_000), run(2, sprint, 580_000), run(3, sprint, 590_000), run(4, sprint, 570_000), run(5, sprint, 585_000), run(6, sprint, 640_000))
        val a = runs.map { Results.award(sprint, it, runs) }
        assertTrue(a[0].debut && a[0].qualifying && a[0].medal == null)
        // A record during qualifying is still a record, but no medal yet.
        assertTrue(a[1].record && a[1].medal == null)
        assertTrue(a[2].qualifying)
        assertEquals(Medal.GOLD, a[3].medal)
        assertTrue(a[3].record)
        assertEquals(Medal.BRONZE, a[4].medal)
        assertEquals(6, a[5].place)
        assertNull(a[5].medal)
    }

    @Test
    fun rushedWinsNothingAndDoesNotRaiseTheBar() {
        val runs = listOf(run(1, sprint, 600_000), run(2, sprint, 610_000), run(3, sprint, 620_000), run(4, sprint, 300_000, form = Form.RUSHED), run(5, sprint, 590_000))
        assertEquals(0, Results.award(sprint, runs[3], runs).place)
        assertNull(Results.award(sprint, runs[3], runs).medal)
        assertEquals(Medal.GOLD, Results.award(sprint, runs[4], runs).medal)
        assertEquals(590_000L, Results.record(sprint, runs)?.duration)
    }

    @Test
    fun marathonAndPaceScoreTheirOwnWay() {
        val p = listOf(run(1, plank, 60_000), run(2, plank, 90_000), run(3, plank, 75_000))
        assertEquals(90_000L, Results.record(plank, p)?.duration)
        val l = listOf(run(1, lunch, 1_500_000), run(2, lunch, 1_150_000), run(3, lunch, 1_250_000))
        assertEquals(1_150_000L, Results.record(lunch, l)?.duration)
        assertTrue(Results.ghost(lunch, l, GhostMode.RECORD, 10 * day)!!.pacer)
    }

    @Test
    fun ghostsAndTheBoard() {
        val runs = listOf(
            run(1, sprint, 600_000, splits = "200000,400000,600000"),
            run(2, sprint, 540_000, splits = "180000,360000,540000"),
            run(3, sprint, 700_000, at = 40 * day, splits = "250000,450000,700000"),
        )
        val rec = Results.ghost(sprint, runs, GhostMode.RECORD, 50 * day)!!
        assertEquals(540_000, rec.total)
        assertEquals(listOf(180_000L, 360_000L, 540_000L), rec.splits)
        assertEquals(700_000, Results.ghost(sprint, runs, GhostMode.LAST, 50 * day)!!.total)
        assertEquals(540_000, Results.ghost(sprint, runs, GhostMode.MONTH, 50 * day)!!.total)
        val std = Results.ghost(sprint, runs, GhostMode.STANDARD, 50 * day)!!
        assertEquals(600_000, std.total)
        assertEquals(600_000, std.splits.last())

        // 10 s early at the first split: minus is ahead; then past the ghost's second mark.
        val b = Results.board(sprint, rec, listOf(170_000), 400_000)
        assertEquals(-10_000L, b.lastSplit)
        assertEquals(360_000, b.mark)
        assertTrue(b.toMark < 0)
        assertEquals(0.5f, Results.ghostAt(rec, 3, 270_000), 0.001f)
    }

    @Test
    fun sectionsAndTheSumOfBest() {
        val runs = listOf(
            run(1, sprint, 600_000, splits = "200000,400000,600000"),
            run(2, sprint, 560_000, splits = "220000,380000,560000"),
        )
        assertEquals(listOf(200_000L, 160_000L, 180_000L), Results.bestSections(sprint, runs))
        assertEquals(540_000L, Results.sumOfBest(sprint, runs))
        assertEquals(listOf(false, true, true), Results.goldSections(sprint, runs[1], runs))
    }

    @Test
    fun gamesCalendarAndFlame() {
        val from = LocalDate.of(2026, 9, 7).toEpochDay()
        val g = GamesCalendar.of(LocalDate.of(2026, 10, 18), from, 4)
        assertEquals(2, g.number)
        assertEquals(LocalDate.of(2026, 10, 5), g.from)
        assertEquals(LocalDate.of(2026, 11, 2), g.to)
        assertEquals(0, GamesCalendar.of(LocalDate.of(2026, 9, 1), from, 4).number)
        assertEquals(g, GamesCalendar.number(2, from, 4))

        val today = LocalDate.of(2026, 10, 18)
        fun at(d: LocalDate) = d.atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val runs = listOf(at(today.minusDays(1)), at(today.minusDays(2)), at(today.minusDays(4))).mapIndexed { i, t -> run(i + 1L, sprint, 1000, at = t) }
        // Not yet today: the flame still counts up to yesterday.
        assertEquals(2, GamesCalendar.flame(runs, today, ZoneOffset.UTC))
    }

    @Test
    fun medalTableByPartOfLife() {
        val home = sprint.copy(ring = Ring.HOME.name)
        val runs = (1..5L).map { run(it, home, 1_000_000 - it * 1000) }
        val placed = GamesCalendar.placed(listOf(home), runs)
        val g = GamesCalendar.of(GamesCalendar.date(3 * day, ZoneOffset.UTC), 0, 52)
        val t = GamesCalendar.table(placed, g, ZoneOffset.UTC)
        assertEquals(2, t[Ring.HOME]?.gold)
    }

    @Test
    fun entriesAndPlateau() {
        val runs = listOf(run(1, sprint, 660_000).copy(guess = 600_000), run(2, sprint, 600_000).copy(guess = 600_000))
        val c = Insights.allCalls(runs)!!
        assertEquals(0.05f, c.bias, 0.001f)
        assertEquals(1, c.close)
        val flat = (1..12L).map { run(it, sprint, if (it == 2L) 500_000 else 600_000 + (it % 3) * 5_000) }
        assertTrue(Insights.form(sprint, flat).plateau)
        assertFalse(Insights.form(sprint, flat.take(4)).plateau)
    }

    @Test
    fun relaysAddUp() {
        val r = Relay(id = 7, name = "Evening", legs = "1,2")
        val runs = listOf(
            Run(id = 1, eventId = 1, start = 100, duration = 600, relay = 100, relayId = 7),
            Run(id = 2, eventId = 2, start = 800, duration = 300, relay = 100, relayId = 7),
            Run(id = 3, eventId = 1, start = 5000, duration = 500, relay = 5000, relayId = 7),
        )
        val all = Relays.of(r, runs)
        assertEquals(2, all.size)
        assertEquals(900L, Relays.best(r, runs)?.total)
        assertFalse(all.first().complete)
    }

    @Test
    fun heatSurvivesAndBackupRoundTrips() {
        val h = Heat(eventId = 3, since = 1000, pausedMs = 50, splits = listOf(10, 20), pits = listOf("PHONE" to 50L), ghost = GhostMode.MONTH, guess = 900)
        assertEquals(h, Heat.decode(h.encode()))
        assertEquals(h.elapsed(2000), 950)
        val all = Everything(listOf(sprint, plank), listOf(run(1, sprint, 600_000, splits = "1,2,3").copy(conditions = "TIRED", pits = "PHONE=5")), listOf(Relay(id = 1, name = "R", legs = "1,2")))
        val back = Backup.decode(Backup.encode(all))
        assertEquals(all.events, back.events)
        assertEquals(all.runs, back.runs)
        assertEquals(all.relays, back.relays)
    }
}
