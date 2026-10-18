package app.podium.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.podium.MainActivity
import app.podium.R
import app.podium.container
import app.podium.domain.GamesCalendar
import app.podium.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one reminder Podium gives: the closing ceremony, the morning after a Games ends. No daily
 * nagging; the stadium is there when you come.
 */
object Alarms {
    private const val CHANNEL = "ceremony_v1"
    const val CHANNEL_LIVE = "heat_v1"
    private const val ACTION = "app.podium.notify.CEREMONY"
    const val EXTRA_OPEN = "open"
    const val EXTRA_AT = "at"
    private const val CEREMONY_HOUR = 9

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, l.getString(R.string.channel_ceremony), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_ceremony_desc)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_LIVE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_LIVE, l.getString(R.string.channel_live), NotificationManager.IMPORTANCE_LOW).apply {
                    description = l.getString(R.string.channel_live_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Morning of the day the next Games opens. */
    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        if (!s.onboarded || !s.ceremony) {
            am.cancel(p)
            return
        }
        val zone = ZoneId.systemDefault()
        val g = GamesCalendar.of(LocalDate.now(zone), s.gamesFrom, s.gamesWeeks)
        val at = g.to.atTime(CEREMONY_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        // A ceremony doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    suspend fun fire(context: Context) {
        val c = context.container
        val s = c.settings.now()
        val g = GamesCalendar.of(LocalDate.now(), s.gamesFrom, s.gamesWeeks)
        val closed = g.number - 1
        val key = "closing:$closed"
        if (closed < 1 || key in s.fired.split(',')) return
        c.settings.update { it.copy(fired = (it.fired.split(',').filter { f -> f.isNotEmpty() }.takeLast(20) + key).joinToString(",")) }
        val dao = c.db.dao()
        val placed = GamesCalendar.placed(dao.events(), dao.runs())
        val games = GamesCalendar.number(closed, s.gamesFrom, s.gamesWeeks)
        val heats = placed.count { GamesCalendar.date(it.run.start) in games }
        if (heats == 0 || !canNotify(context)) return
        val tally = GamesCalendar.table(placed, games).values.fold(app.podium.domain.Tally()) { a, b -> a + b }
        val l = context.localized()
        val text = l.getString(R.string.ceremony_note_text, tally.gold, tally.silver, tally.bronze, l.resources.getQuantityString(R.plurals.heats, heats, heats))
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(l.getString(R.string.ceremony_note_title, closed))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, "games:$closed", 100))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(10, n)
        } catch (_: SecurityException) {
        }
    }

    fun open(context: Context, open: String, code: Int): PendingIntent = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN, open)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal const val CEREMONY = ACTION
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alarms.CEREMONY) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context)
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.reschedule(context)
                // A reboot clears the heat's notification; bring it back.
                val h = context.container.settings.now().heat
                if (h.active) Live.show(context, h)
            } finally {
                pending.finish()
            }
        }
    }
}
