package app.podium.data

import android.content.Context
import app.podium.container
import app.podium.notify.Live

/** The legs to go once a heat finishes: the next one starts on the spot. */
sealed interface Finished {
    data class Done(val runId: Long) : Finished
    data class NextLeg(val runId: Long, val next: Long) : Finished
}

/**
 * Starting, splitting, pausing and finishing the heat under way. The heat lives in the settings, so
 * it survives the app being closed; the notification keeps its clock in the shade.
 */
object Heats {
    suspend fun start(context: Context, eventId: Long, ghost: GhostMode, guess: Long = 0, relay: Relay? = null) {
        val c = context.container
        val now = System.currentTimeMillis()
        val h = Heat(eventId = eventId, since = now, ghost = ghost, guess = guess, relayId = relay?.id ?: 0, relayKey = if (relay != null) now else 0)
        c.settings.update { it.copy(heat = h) }
        Live.show(context, h)
    }

    /** Takes a split; when it's the last section, finishes instead. */
    suspend fun split(context: Context, now: Long = System.currentTimeMillis()): Finished? {
        val c = context.container
        val h = c.settings.now().heat
        if (!h.active || h.paused) return null
        val e = c.db.dao().event(h.eventId) ?: return null
        if (h.splits.size + 1 >= e.sections.size) return finish(context, now)
        val next = h.copy(splits = h.splits + h.elapsed(now))
        c.settings.update { it.copy(heat = next) }
        Live.show(context, next)
        return null
    }

    suspend fun pause(context: Context) {
        val c = context.container
        val h = c.settings.now().heat
        if (!h.active || h.paused) return
        val next = h.copy(pausedAt = System.currentTimeMillis())
        c.settings.update { it.copy(heat = next) }
        Live.show(context, next)
    }

    /** Back from the pits; [reason] is what took the time. */
    suspend fun resume(context: Context, reason: String = "OTHER") {
        val c = context.container
        val h = c.settings.now().heat
        if (!h.active || !h.paused) return
        val stop = System.currentTimeMillis() - h.pausedAt
        val next = h.copy(pausedAt = 0, pausedMs = h.pausedMs + stop, pits = h.pits + (reason to stop))
        c.settings.update { it.copy(heat = next) }
        Live.show(context, next)
    }

    /** Ends the heat and keeps it. In a relay the next leg starts at once. */
    suspend fun finish(context: Context, now: Long = System.currentTimeMillis()): Finished? {
        val c = context.container
        val dao = c.db.dao()
        val h = c.settings.now().heat
        if (!h.active) return null
        val t = h.elapsed(now)
        val e = dao.event(h.eventId)
        val splits = if (e != null && e.sections.size >= 2) h.splits + t else emptyList()
        // Finished from a pit stop: the stop counts as one.
        val all = if (h.paused) h.pits + ("OTHER" to now - h.pausedAt) else h.pits
        val pits = all.groupBy({ it.first }, { it.second }).map { (k, v) -> "$k=${v.sum()}" }.joinToString(";")
        val id = dao.insert(
            Run(
                eventId = h.eventId, start = h.since, duration = t, splits = splits.joinToString(","),
                pitMs = all.sumOf { it.second }, pits = pits,
                guess = h.guess, relay = h.relayKey, relayId = h.relayId,
            )
        )
        val relay = if (h.relayId > 0) dao.relay(h.relayId) else null
        val nextLeg = relay?.legIds?.getOrNull(h.leg + 1)?.takeIf { dao.event(it) != null }
        if (relay != null && nextLeg != null) {
            val next = Heat(eventId = nextLeg, since = now, ghost = h.ghost, relayId = relay.id, relayKey = h.relayKey, leg = h.leg + 1)
            c.settings.update { it.copy(heat = next) }
            Live.show(context, next)
            return Finished.NextLeg(id, nextLeg)
        }
        c.settings.update { it.copy(heat = Heat()) }
        Live.hide(context)
        c.changed()
        return Finished.Done(id)
    }

    /** Throws the heat away; legs already run in a relay stay. */
    suspend fun cancel(context: Context) {
        val c = context.container
        c.settings.update { it.copy(heat = Heat()) }
        Live.hide(context)
        c.changed()
    }
}
