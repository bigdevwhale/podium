package app.podium.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.podium.MainActivity
import app.podium.R
import app.podium.data.Event
import app.podium.data.Run

/** A tile in the quick settings: to the start line from anywhere, two swipes away. */
class StartTile : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val open = Intent(this, MainActivity::class.java).putExtra(Alarms.EXTRA_OPEN, "start")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 21, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(open)
        }
    }
}

/** The disciplines raced most lately, on a long press of the launcher icon: one tap to the start. */
object Shortcuts {
    fun update(context: Context, events: List<Event>, runs: List<Run>) {
        val recent = runs.sortedByDescending { it.start }.take(40).groupingBy { it.eventId }.eachCount()
        val top = events.filter { !it.retired }.sortedByDescending { recent[it.id] ?: 0 }.take(3)
        val list = top.mapIndexed { i, e ->
            ShortcutInfoCompat.Builder(context, "event-${e.id}")
                .setShortLabel(e.name.take(24))
                .setLongLabel(e.name)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut))
                .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(Alarms.EXTRA_OPEN, "go:${e.id}"))
                .setRank(i)
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, list) }
    }
}
