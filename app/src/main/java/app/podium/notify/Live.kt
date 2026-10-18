package app.podium.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.podium.MainActivity
import app.podium.R
import app.podium.container
import app.podium.data.Heat
import app.podium.data.Heats
import app.podium.domain.Results
import app.podium.localized
import app.podium.ui.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The heat under way, in the shade: its clock, the ghost to beat, and buttons for the split, the pit
 * stop and the finish, so the phone can stay locked in a pocket.
 */
object Live {
    private const val ID = 1
    const val ACTION_SPLIT = "app.podium.SPLIT"
    const val ACTION_PAUSE = "app.podium.PAUSE"
    const val ACTION_RESUME = "app.podium.RESUME"

    suspend fun show(context: Context, h: Heat) {
        if (!Alarms.canNotify(context) || !h.active) return
        val dao = context.container.db.dao()
        val e = dao.event(h.eventId) ?: return
        val l = context.localized()
        val n = e.sections.size
        val sectionText = if (n >= 2) l.getString(R.string.live_section, h.splits.size + 1, n, e.sections[h.splits.size.coerceAtMost(n - 1)]) else null
        val ghost = Results.ghost(e, dao.runsOf(e.id), h.ghost, h.since)
        val board = Results.board(e, ghost, h.splits, h.elapsed(System.currentTimeMillis()))
        val text = when {
            h.paused -> l.getString(R.string.live_paused, T.stopwatch(h.elapsed(System.currentTimeMillis())))
            board.lastSplit != null -> l.getString(R.string.live_last_split, T.delta(board.lastSplit))
            ghost != null -> l.getString(if (ghost.pacer) R.string.live_target else R.string.live_ghost, T.stopwatch(ghost.total))
            else -> l.getString(R.string.live_first)
        }
        val b = NotificationCompat.Builder(context, Alarms.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(listOfNotNull(e.name, sectionText).joinToString(" · "))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(Alarms.open(context, "heat", 99))
        if (h.paused) {
            b.setShowWhen(false)
            b.addAction(0, l.getString(R.string.live_resume), broadcast(context, ACTION_RESUME, 2))
        } else {
            b.setWhen(h.since + h.pausedMs).setUsesChronometer(true).setShowWhen(true)
            // Sections are run to the end: the finish comes only after the last split.
            if (n >= 2 && h.splits.size + 1 < n) b.addAction(0, l.getString(R.string.heat_split), broadcast(context, ACTION_SPLIT, 1))
            else b.addAction(0, l.getString(R.string.heat_finish), Alarms.open(context, "finish", 98))
            b.addAction(0, l.getString(R.string.heat_pit), broadcast(context, ACTION_PAUSE, 3))
        }
        try {
            NotificationManagerCompat.from(context).notify(ID, b.build())
        } catch (_: SecurityException) {
        }
    }

    fun hide(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    private fun broadcast(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, LiveReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private val liveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class LiveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val at = System.currentTimeMillis()
        val pending = goAsync()
        liveScope.launch {
            try {
                when (intent.action) {
                    Live.ACTION_SPLIT -> Heats.split(context, at)
                    Live.ACTION_PAUSE -> Heats.pause(context)
                    Live.ACTION_RESUME -> Heats.resume(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
