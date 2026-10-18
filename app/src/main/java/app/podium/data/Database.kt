package app.podium.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Which way a discipline is won. */
enum class Sport {
    /** Faster wins: the morning routine, the kitchen after dinner. */
    SPRINT,

    /** Longer wins: a plank, reading without the phone, a focus session. */
    MARATHON,

    /** Closest to a set time wins: lunch without hurrying, a walk of exactly half an hour. */
    PACE,
}

/** The part of life a discipline belongs to; the medal table is kept by these instead of by nations. */
enum class Ring { HOME, BODY, WORK, STUDY, SELF, OTHER }

/** How it was done. A rushed heat stays in the protocol but wins nothing. */
enum class Form { CLEAN, OK, RUSHED }

/** What the day was like; the stats compare heats with and without each. */
enum class Condition { FRESH, TIRED, INTERRUPTED, UNWELL, COFFEE, NEW_PLACE }

/**
 * Something done again and again, raced against your own past. [splits] are the names of its
 * sections, one per line, or empty for a single stretch.
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [Sport] name. */
    val sport: String = Sport.SPRINT.name,
    /** [Ring] name. */
    val ring: String = Ring.HOME.name,
    /** Index into the pictograms. */
    val picto: Int = 0,
    val splits: String = "",
    /** For [Sport.PACE]: the time to hit, ms. */
    val target: Long = 0,
    /** Retired: kept with its records, out of the way. */
    val retired: Boolean = false,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val sportOf: Sport get() = runCatching { Sport.valueOf(sport) }.getOrDefault(Sport.SPRINT)
    val ringOf: Ring get() = runCatching { Ring.valueOf(ring) }.getOrDefault(Ring.OTHER)
    val sections: List<String> get() = splits.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * One heat. [splits] are the elapsed times at each section's end, ms, comma separated, the last
 * one being the finish; [duration] is the time on the clock, without pit stops.
 */
@Entity(tableName = "runs", indices = [Index("eventId"), Index("start")])
data class Run(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val start: Long,
    val duration: Long,
    val splits: String = "",
    /** Time spent in pit stops, ms, and why: "PHONE=60000;PEOPLE=30000". */
    val pitMs: Long = 0,
    val pits: String = "",
    /** The time entered before the start, ms, or 0. */
    val guess: Long = 0,
    /** [Form] name. */
    val form: String = Form.CLEAN.name,
    /** [Condition] names, comma separated. */
    val conditions: String = "",
    val note: String = "",
    /** The start of the relay this heat was a leg of, or 0. */
    val relay: Long = 0,
    val relayId: Long = 0,
    /** Entered by hand afterwards rather than timed. */
    val manual: Boolean = false,
) {
    val formOf: Form get() = runCatching { Form.valueOf(form) }.getOrDefault(Form.CLEAN)
    val counts: Boolean get() = formOf != Form.RUSHED
    val splitTimes: List<Long> get() = splits.split(',').mapNotNull { it.trim().toLongOrNull() }
    val conditionSet: Set<Condition> get() = conditions.split(',').mapNotNull { c -> Condition.entries.firstOrNull { it.name == c.trim() } }.toSet()
    val pitList: List<Pair<String, Long>>
        get() = pits.split(';').mapNotNull { e -> e.substringAfter('=').toLongOrNull()?.let { e.substringBefore('=') to it } }

    /** Section times, ms; one per split. */
    val sections: List<Long> get() = splitTimes.mapIndexed { i, t -> t - (splitTimes.getOrNull(i - 1) ?: 0L) }

    companion object {
        fun conditionsOf(set: Collection<Condition>) = set.sortedBy { it.ordinal }.joinToString(",") { it.name }
    }
}

/** Several disciplines in a row, the baton passed from one to the next: the morning, the evening tidy-up. */
@Entity(tableName = "relays")
data class Relay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Event ids in order, comma separated. */
    val legs: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val legIds: List<Long> get() = legs.split(',').mapNotNull { it.trim().toLongOrNull() }
}

@Dao
abstract class PodiumDao {
    @Query("SELECT * FROM events ORDER BY `order` ASC, id ASC") abstract fun observeEvents(): Flow<List<Event>>
    @Query("SELECT * FROM runs ORDER BY start DESC") abstract fun observeRuns(): Flow<List<Run>>
    @Query("SELECT * FROM relays ORDER BY id ASC") abstract fun observeRelays(): Flow<List<Relay>>

    @Query("SELECT * FROM events ORDER BY `order` ASC, id ASC") abstract suspend fun events(): List<Event>
    @Query("SELECT * FROM runs ORDER BY start DESC") abstract suspend fun runs(): List<Run>
    @Query("SELECT * FROM relays ORDER BY id ASC") abstract suspend fun relays(): List<Relay>
    @Query("SELECT * FROM events WHERE id = :id") abstract suspend fun event(id: Long): Event?
    @Query("SELECT * FROM runs WHERE id = :id") abstract suspend fun run(id: Long): Run?
    @Query("SELECT * FROM runs WHERE eventId = :id ORDER BY start DESC") abstract suspend fun runsOf(id: Long): List<Run>
    @Query("SELECT * FROM runs WHERE relay = :key ORDER BY start ASC") abstract suspend fun legsOf(key: Long): List<Run>
    @Query("SELECT * FROM relays WHERE id = :id") abstract suspend fun relay(id: Long): Relay?

    @Insert abstract suspend fun insert(e: Event): Long
    @Update abstract suspend fun update(e: Event)
    @Delete abstract suspend fun delete(e: Event)

    @Insert abstract suspend fun insert(r: Run): Long
    @Update abstract suspend fun update(r: Run)
    @Delete abstract suspend fun delete(r: Run)

    @Insert abstract suspend fun insert(r: Relay): Long
    @Update abstract suspend fun update(r: Relay)
    @Delete abstract suspend fun delete(r: Relay)

    @Query("DELETE FROM runs WHERE eventId = :id") abstract suspend fun deleteRunsOf(id: Long)

    @Query("DELETE FROM events") protected abstract suspend fun clearEvents()
    @Query("DELETE FROM runs") protected abstract suspend fun clearRuns()
    @Query("DELETE FROM relays") protected abstract suspend fun clearRelays()
    @Insert protected abstract suspend fun insertEvents(l: List<Event>)
    @Insert protected abstract suspend fun insertRuns(l: List<Run>)
    @Insert protected abstract suspend fun insertRelays(l: List<Relay>)

    @Transaction
    open suspend fun replaceAll(all: Everything) {
        clearEvents(); clearRuns(); clearRelays()
        insertEvents(all.events); insertRuns(all.runs); insertRelays(all.relays)
    }
}

/** The whole stadium, for backups. */
class Everything(val events: List<Event>, val runs: List<Run>, val relays: List<Relay>)

@Database(entities = [Event::class, Run::class, Relay::class], version = 1, exportSchema = true)
abstract class PodiumDatabase : RoomDatabase() {
    abstract fun dao(): PodiumDao

    companion object {
        fun create(context: Context): PodiumDatabase =
            Room.databaseBuilder(context.applicationContext, PodiumDatabase::class.java, "podium.db").build()
    }
}
