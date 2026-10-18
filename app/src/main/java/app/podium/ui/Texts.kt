package app.podium.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.podium.R
import app.podium.data.Condition
import app.podium.data.Form
import app.podium.data.GhostMode
import app.podium.data.Ring
import app.podium.data.Sport
import app.podium.domain.Medal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Words, times and dates in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    /** The proper minus, not a hyphen: it's a scoreboard. */
    const val MINUS = "−"

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    /** "0:42", "12:05", "1:05:09": a clock. */
    fun stopwatch(ms: Long): String {
        val s = abs(ms) / 1000
        return if (s < 3600) "%d:%02d".format(s / 60, s % 60) else "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    }

    /** With tenths under ten minutes, where they decide things: "4:07.3". */
    fun precise(ms: Long): String {
        val a = abs(ms)
        return if (a >= 600_000) stopwatch(a) else "%d:%02d.%d".format(a / 60_000, a / 1000 % 60, a / 100 % 10)
    }

    /** "−0:12.3", "+1:05": against the ghost, minus being earlier. */
    fun delta(ms: Long): String = (if (ms < 0) MINUS else "+") + precise(ms)

    /** "+18%", "−7%". */
    fun percent(f: Float): String {
        val p = (f * 100).roundToInt()
        return (if (p < 0) MINUS else "+") + abs(p) + "%"
    }

    /** "17 Oct 2026". */
    fun date(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(c)))
    fun date(c: Context, ms: Long): String = date(c, local(ms).toLocalDate())

    /** "17 Oct". */
    fun day(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM", locale(c)))
    fun day(c: Context, ms: Long): String = day(c, local(ms).toLocalDate())

    /** "Saturday, 17 Oct". */
    fun weekday(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    fun time(c: Context, ms: Long): String = local(ms).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale(c)))

    /** "17 Oct – 13 Nov". */
    fun range(c: Context, from: LocalDate, toExclusive: LocalDate): String = day(c, from) + " – " + day(c, toExclusive.minusDays(1))

    fun sport(c: Context, s: Sport): String = c.getString(
        when (s) {
            Sport.SPRINT -> R.string.sport_sprint
            Sport.MARATHON -> R.string.sport_marathon
            Sport.PACE -> R.string.sport_pace
        }
    )

    fun sportRule(c: Context, s: Sport): String = c.getString(
        when (s) {
            Sport.SPRINT -> R.string.sport_sprint_rule
            Sport.MARATHON -> R.string.sport_marathon_rule
            Sport.PACE -> R.string.sport_pace_rule
        }
    )

    fun ring(c: Context, r: Ring): String = c.getString(
        when (r) {
            Ring.HOME -> R.string.ring_home
            Ring.BODY -> R.string.ring_body
            Ring.WORK -> R.string.ring_work
            Ring.STUDY -> R.string.ring_study
            Ring.SELF -> R.string.ring_self
            Ring.OTHER -> R.string.ring_other
        }
    )

    fun form(c: Context, f: Form): String = c.getString(
        when (f) {
            Form.CLEAN -> R.string.form_clean
            Form.OK -> R.string.form_ok
            Form.RUSHED -> R.string.form_rushed
        }
    )

    fun condition(c: Context, k: Condition): String = c.getString(
        when (k) {
            Condition.FRESH -> R.string.cond_fresh
            Condition.TIRED -> R.string.cond_tired
            Condition.INTERRUPTED -> R.string.cond_interrupted
            Condition.UNWELL -> R.string.cond_unwell
            Condition.COFFEE -> R.string.cond_coffee
            Condition.NEW_PLACE -> R.string.cond_new_place
        }
    )

    fun ghost(c: Context, g: GhostMode): String = c.getString(
        when (g) {
            GhostMode.RECORD -> R.string.ghost_record
            GhostMode.LAST -> R.string.ghost_last
            GhostMode.STANDARD -> R.string.ghost_standard
            GhostMode.MONTH -> R.string.ghost_month
        }
    )

    fun ghostSub(c: Context, g: GhostMode): String = c.getString(
        when (g) {
            GhostMode.RECORD -> R.string.ghost_record_sub
            GhostMode.LAST -> R.string.ghost_last_sub
            GhostMode.STANDARD -> R.string.ghost_standard_sub
            GhostMode.MONTH -> R.string.ghost_month_sub
        }
    )

    /** The reasons a pit stop can have. */
    val pitReasons = listOf("PHONE", "PEOPLE", "BREAK", "OTHER")

    fun pit(c: Context, reason: String): String = c.getString(
        when (reason) {
            "PHONE" -> R.string.pit_phone
            "PEOPLE" -> R.string.pit_people
            "BREAK" -> R.string.pit_break
            else -> R.string.pit_other
        }
    )

    fun medal(c: Context, m: Medal): String = c.getString(
        when (m) {
            Medal.GOLD -> R.string.medal_gold
            Medal.SILVER -> R.string.medal_silver
            Medal.BRONZE -> R.string.medal_bronze
        }
    )

    /** "1st", "2-е": a place in the protocol. */
    fun place(c: Context, n: Int): String = c.getString(R.string.place_n, n)
}

@Composable
fun ctx(): Context = LocalContext.current
