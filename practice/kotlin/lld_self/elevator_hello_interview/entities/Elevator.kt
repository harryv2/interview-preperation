package lld_self.elevator_hello_interview.entities

import lld_self.elevator_hello_interview.strategies.ElevatorMovementStrategy
import lld_self.elevator_hello_interview.strategies.Stop


enum class ElevatorState {
    STOPPED,
    MOVING
}

enum class Direction(val symbol: String) {
    UP("^"),
    DOWN("v"),
    IDLE("-")
}

class Elevator(
    val name: String,
    val movementStrategy: ElevatorMovementStrategy
) {
    private val observers = mutableListOf<ElevatorObserver>()

    val door = Door { doorState ->
        observers.forEach { it.onDoorStateChanged(this, doorState) }
    }

    val elevatorPanel = ElevatorPanel(this)

    val display = CabinDisplay()

    var direction: Direction = Direction.IDLE
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            observers.forEach { it.onDirectionChanged(this, value) }
        }

    var currentFloor: Int = 0
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            observers.forEach { it.onFloorChanged(this, value) }
        }

    var state: ElevatorState = ElevatorState.STOPPED
        private set

    private val requests = mutableSetOf<Request>()

    val pendingRequests: Set<Request>
        get() = requests

    init {
        addObserver(display)
    }

    fun addObserver(observer: ElevatorObserver) {
        observers.add(observer)
    }

    fun addRequest(request: Request): Boolean {
        if (request.floor == currentFloor && canServeHereNow(request)) {
            if (direction == Direction.IDLE) {
                direction = request.direction
            }
            door.open()
            return true
        }

        return requests.add(request)
    }

    fun step() {
        door.step()

        if (door.state != DoorState.CLOSED) {
            return
        }

        val next = movementStrategy.nextStop(currentFloor, direction, requests)

        if (next == null) {
            direction = Direction.IDLE
            state = ElevatorState.STOPPED
            return
        }

        if (next.floor == currentFloor) {
            arrive(next)
            return
        }

        direction = if (next.floor > currentFloor) Direction.UP else Direction.DOWN
        state = ElevatorState.MOVING
        currentFloor += if (direction == Direction.UP) 1 else -1
    }

    private fun canServeHereNow(request: Request): Boolean {
        return direction == Direction.IDLE || request.servesDirection(direction)
    }

    private fun arrive(stop: Stop) {
        direction = stop.direction
        requests.removeAll {
            it.floor == stop.floor && (stop.direction == Direction.IDLE || it.servesDirection(stop.direction))
        }
        state = ElevatorState.STOPPED
        door.open()
    }
}
