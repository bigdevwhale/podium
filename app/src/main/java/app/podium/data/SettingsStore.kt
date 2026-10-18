package app.podium.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("settings")

/** Who runs in the next lane. */
enum class GhostMode {
    /** Your record. */
    RECORD,

    /** You, the last time. */
    LAST,

    /** The qualifying standard: your median, the ordinary you. */
    STANDARD,

    /** You a month ago, to see how far you've come. */
    MONTH,
}

/**
 * A heat under way. The clock is [since] to now (or to [pausedAt] in a pit stop), less [pausedMs];
 * [splits] are the elapsed times already taken. A relay carries its [relayKey] and the leg it's on.
 */
data class Heat(
    val eventId: Long = 0,
    val since: Long = 0,
    val pausedAt: Long = 0,
    val pausedMs: Long = 0,
    val splits: List<Long> = emptyList(),
    /** Pit stops so far: reason name to ms. */
    val pits: List<Pair<String, Long>> = emptyList(),
    val ghost: GhostMode = GhostMode.RECORD,
    val guess: Long = 0,
    val relayId: Long = 0,
    val relayKey: Long = 0,
    val leg: Int = 0,
) {
    val active: Boolean get() = since > 0
    val paused: Boolean get() = pausedAt > 0

    fun elapsed(now: Long): Long = ((if (paused) pausedAt else now) - since - pausedMs).coerceAtLeast(0)

    fun encode(): String = JSONObject()
        .put("eventId", eventId).put("since", since).put("pausedAt", pausedAt).put("pausedMs", pausedMs)
        .put("splits", splits.joinToString(","))
        .put("pits", pits.joinToString(";") { "${it.first}=${it.second}" })
        .put("ghost", ghost.name).put("guess", guess)
        .put("relayId", relayId).put("relayKey", relayKey).put("leg", leg)
        .toString()

    companion object {
        fun decode(s: String?): Heat {
            if (s.isNullOrEmpty()) return Heat()
            val o = runCatching { JSONObject(s) }.getOrNull() ?: return Heat()
            return Heat(
                eventId = o.optLong("eventId"),
                since = o.optLong("since"),
                pausedAt = o.optLong("pausedAt"),
                pausedMs = o.optLong("pausedMs"),
                splits = o.optString("splits").split(',').mapNotNull { it.toLongOrNull() },
                pits = o.optString("pits").split(';').mapNotNull { e -> e.substringAfter('=').toLongOrNull()?.let { e.substringBefore('=') to it } },
                ghost = runCatching { GhostMode.valueOf(o.optString("ghost")) }.getOrDefault(GhostMode.RECORD),
                guess = o.optLong("guess"),
                relayId = o.optLong("relayId"),
                relayKey = o.optLong("relayKey"),
                leg = o.optInt("leg"),
            )
        }
    }
}

data class Settings(
    val onboarded: Boolean = false,
    /** The epoch day of the Monday the first Games opened. */
    val gamesFrom: Long = 0,
    /** How long one Games lasts, in weeks. */
    val gamesWeeks: Int = 4,
    val ghost: GhostMode = GhostMode.RECORD,
    /** A buzz at each split and when you overtake the ghost. */
    val haptics: Boolean = true,
    /** The closing ceremony when a Games ends. */
    val ceremony: Boolean = true,
    val heat: Heat = Heat(),
    /** The Games whose closing ceremony has been seen. */
    val closedSeen: Int = 0,
    /** Reminders already given ("closing:3"), so none comes twice. */
    val fired: String = "",
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        gamesFrom = this[GAMES_FROM] ?: 0L,
        gamesWeeks = this[GAMES_WEEKS] ?: 4,
        ghost = runCatching { GhostMode.valueOf(this[GHOST] ?: "") }.getOrDefault(GhostMode.RECORD),
        haptics = this[HAPTICS] ?: true,
        ceremony = this[CEREMONY] ?: true,
        heat = Heat.decode(this[HEAT]),
        closedSeen = this[CLOSED_SEEN] ?: 0,
        fired = this[FIRED] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[GAMES_FROM] = s.gamesFrom
            it[GAMES_WEEKS] = s.gamesWeeks
            it[GHOST] = s.ghost.name
            it[HAPTICS] = s.haptics
            it[CEREMONY] = s.ceremony
            it[HEAT] = if (s.heat.active) s.heat.encode() else ""
            it[CLOSED_SEEN] = s.closedSeen
            it[FIRED] = s.fired
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GAMES_FROM = longPreferencesKey("games_from")
        val GAMES_WEEKS = intPreferencesKey("games_weeks")
        val GHOST = stringPreferencesKey("ghost")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CEREMONY = booleanPreferencesKey("ceremony")
        val HEAT = stringPreferencesKey("heat")
        val CLOSED_SEEN = intPreferencesKey("closed_seen")
        val FIRED = stringPreferencesKey("fired")
    }
}
