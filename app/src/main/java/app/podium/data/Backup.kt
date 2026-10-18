package app.podium.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Every discipline, heat and relay in one JSON file. Nothing leaves the phone unless you export it. */
object Backup {
    private const val VERSION = 1

    fun encode(all: Everything): String = JSONObject()
        .put("app", "podium")
        .put("version", VERSION)
        .put("events", JSONArray(all.events.map {
            JSONObject().put("id", it.id).put("name", it.name).put("sport", it.sport).put("ring", it.ring).put("picto", it.picto)
                .put("splits", it.splits).put("target", it.target).put("retired", it.retired).put("order", it.order).put("createdAt", it.createdAt)
        }))
        .put("runs", JSONArray(all.runs.map {
            JSONObject().put("id", it.id).put("eventId", it.eventId).put("start", it.start).put("duration", it.duration).put("splits", it.splits)
                .put("pitMs", it.pitMs).put("pits", it.pits).put("guess", it.guess).put("form", it.form).put("conditions", it.conditions)
                .put("note", it.note).put("relay", it.relay).put("relayId", it.relayId).put("manual", it.manual)
        }))
        .put("relays", JSONArray(all.relays.map {
            JSONObject().put("id", it.id).put("name", it.name).put("legs", it.legs).put("createdAt", it.createdAt)
        }))
        .toString(2)

    private fun JSONObject.list(key: String) = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()

    /** Throws [IllegalArgumentException] when the text is not a Podium backup. */
    fun decode(text: String): Everything {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "podium") { "Not a Podium backup" }
        return Everything(
            events = root.list("events").map {
                Event(
                    id = it.getLong("id"), name = it.getString("name"), sport = it.optString("sport", Sport.SPRINT.name), ring = it.optString("ring", Ring.OTHER.name),
                    picto = it.optInt("picto"), splits = it.optString("splits"), target = it.optLong("target"), retired = it.optBoolean("retired"),
                    order = it.optInt("order"), createdAt = it.optLong("createdAt"),
                )
            },
            runs = root.list("runs").map {
                Run(
                    id = it.getLong("id"), eventId = it.getLong("eventId"), start = it.getLong("start"), duration = it.getLong("duration"),
                    splits = it.optString("splits"), pitMs = it.optLong("pitMs"), pits = it.optString("pits"), guess = it.optLong("guess"),
                    form = it.optString("form", Form.CLEAN.name), conditions = it.optString("conditions"), note = it.optString("note"),
                    relay = it.optLong("relay"), relayId = it.optLong("relayId"), manual = it.optBoolean("manual"),
                )
            },
            relays = root.list("relays").map { Relay(id = it.getLong("id"), name = it.getString("name"), legs = it.optString("legs"), createdAt = it.optLong("createdAt")) },
        )
    }

    fun write(context: Context, uri: Uri, all: Everything) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(encode(all).toByteArray()) } ?: error("Can't write there")
    }

    fun read(context: Context, uri: Uri): Everything {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't read that")
        return decode(text)
    }
}
