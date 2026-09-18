package lld_self.elevator.entities

enum class DoorState {
    OPEN,
    CLOSED,
    OPENING,
    CLOSING
}

class Door {
    var state = DoorState.CLOSED
        private set

    private val ticksToOpenClose = 3
    private val ticksToCloseAfterOpen = 10
    private var ticksInState = 0

    fun open() {
        when (state) {
            DoorState.CLOSED -> {
                transition(DoorState.OPENING)
            }
            DoorState.CLOSING -> {
                val remaining = ticksToOpenClose - ticksInState
                transition(DoorState.OPENING)
                ticksInState = remaining
            }
            DoorState.OPEN -> {
                ticksInState = 0
            }
            DoorState.OPENING -> {
            }
        }
    }

    fun step() {
        ticksInState++
        when (state) {
            DoorState.OPENING -> {
                if (ticksInState == ticksToOpenClose) {
                    transition(DoorState.OPEN)
                }
            }
            DoorState.OPEN -> {
                if (ticksInState == ticksToCloseAfterOpen) {
                    transition(DoorState.CLOSING)
                }
            }
            DoorState.CLOSING -> {
                if (ticksInState == ticksToOpenClose) {
                    transition(DoorState.CLOSED)
                }
            }
            DoorState.CLOSED -> {
            }
        }
    }

    private fun transition(to: DoorState) {
        state = to
        ticksInState = 0
    }
}