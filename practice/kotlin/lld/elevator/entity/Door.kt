package lld.elevator.entity

// =====================================================================
//  DOOR
//
//  Its own class because it has four states, a timer, and rules about
//  the transitions. If it were only OPEN and CLOSED it would be one
//  field on Elevator and this class would not exist.
// =====================================================================

enum class DoorState { CLOSED, OPENING, OPEN, CLOSING }

class Door(
    private val transitionTicks: Int = 1,
    private val holdOpenTicks: Int = 2
) {
    var state: DoorState = DoorState.CLOSED
        private set

    private var ticksRemaining: Int = 0

    fun open() {
        if (state == DoorState.OPEN || state == DoorState.OPENING) return
        state = DoorState.OPENING
        ticksRemaining = transitionTicks
    }

    fun close() {
        if (state == DoorState.CLOSED || state == DoorState.CLOSING) return
        state = DoorState.CLOSING
        ticksRemaining = transitionTicks
    }

    fun isClosed(): Boolean = state == DoorState.CLOSED

    fun step() {
        if (ticksRemaining > 0) {
            ticksRemaining--
            return
        }
        when (state) {
            DoorState.OPENING -> {
                state = DoorState.OPEN
                ticksRemaining = holdOpenTicks
            }
            DoorState.OPEN -> close()
            DoorState.CLOSING -> state = DoorState.CLOSED
            DoorState.CLOSED -> Unit
        }
    }
}
