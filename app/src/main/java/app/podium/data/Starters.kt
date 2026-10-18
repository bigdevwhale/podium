package app.podium.data

import android.content.Context
import app.podium.R

/** Disciplines to begin with, offered on the welcome screen; names and sections in the app's language. */
object Starters {
    class Starter(val name: Int, val sport: Sport, val ring: Ring, val picto: Int, val sections: Int = 0, val targetMin: Int = 0, val picked: Boolean = false)

    val all = listOf(
        Starter(R.string.st_morning, Sport.SPRINT, Ring.SELF, 16, R.string.st_morning_s, picked = true),
        Starter(R.string.st_kitchen, Sport.SPRINT, Ring.HOME, 12, R.string.st_kitchen_s, picked = true),
        Starter(R.string.st_tidy, Sport.SPRINT, Ring.HOME, 10, R.string.st_tidy_s),
        Starter(R.string.st_inbox, Sport.SPRINT, Ring.WORK, 9),
        Starter(R.string.st_shop, Sport.SPRINT, Ring.HOME, 13),
        Starter(R.string.st_plank, Sport.MARATHON, Ring.BODY, 5, picked = true),
        Starter(R.string.st_read, Sport.MARATHON, Ring.STUDY, 8),
        Starter(R.string.st_focus, Sport.MARATHON, Ring.WORK, 9),
        Starter(R.string.st_lunch, Sport.PACE, Ring.SELF, 11, targetMin = 20),
        Starter(R.string.st_run, Sport.SPRINT, Ring.BODY, 0),
    )

    fun event(c: Context, s: Starter): Event = Event(
        name = c.getString(s.name),
        sport = s.sport.name,
        ring = s.ring.name,
        picto = s.picto,
        splits = if (s.sections != 0) c.getString(s.sections).split('|').joinToString("\n") else "",
        target = s.targetMin * 60_000L,
    )
}
