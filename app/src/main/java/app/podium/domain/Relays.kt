package app.podium.domain

import app.podium.data.Relay
import app.podium.data.Run

/** One running of a relay: its legs in order and the time of them all. */
data class RelayRun(val key: Long, val legs: List<Run>, val total: Long, val complete: Boolean) {
    val counts: Boolean get() = complete && legs.all { it.counts }

    /** Elapsed time at each handover, ms. */
    val handovers: List<Long> get() = legs.map { it.duration }.runningReduce(Long::plus)
}

object Relays {
    /** Every running of [relay], newest first. */
    fun of(relay: Relay, runs: List<Run>): List<RelayRun> =
        runs.filter { it.relayId == relay.id && it.relay > 0 }.groupBy { it.relay }
            .map { (k, l) ->
                val legs = l.sortedBy { it.start }
                RelayRun(k, legs, legs.sumOf { it.duration }, legs.size == relay.legIds.size)
            }
            .sortedByDescending { it.key }

    fun best(relay: Relay, runs: List<Run>): RelayRun? = of(relay, runs).filter { it.counts }.minByOrNull { it.total }
}
