package lld.messaging.oncallalerting.entity

import java.time.Duration


// One rung of the matrix: who gets woken, how they get woken, and how long they have before the page moves on
class EscalationLevel(
    val responders: List<Responder>,
    val channels: List<ChannelType>,
    val waitFor: Duration
) {

    init {
        require(responders.isNotEmpty()) { "An escalation level with nobody on it is a hole in the matrix" }
        require(channels.isNotEmpty()) { "An escalation level needs at least one channel" }
        require(!waitFor.isNegative && !waitFor.isZero) { "waitFor must be positive" }
    }

    override fun toString(): String = "$responders via $channels, ${waitFor.toMillis()}ms to ack"
}


class EscalationPolicy(val levels: List<EscalationLevel>) {

    init {
        require(levels.isNotEmpty()) { "A policy needs at least one level" }
    }

    val lastLevel = levels.size - 1

    fun levelAt(index: Int): EscalationLevel = levels[index]

    fun knows(responder: Responder): Boolean {
        return levels.any { level -> level.responders.any { it.id == responder.id } }
    }
}


class Project(
    val id: String,
    val name: String,
    val policy: EscalationPolicy
) {

    override fun toString(): String = name
}
