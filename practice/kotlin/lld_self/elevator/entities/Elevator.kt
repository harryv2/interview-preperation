package lld_self.elevator.entities

import lld_self.elevator.strategies.ElevatorMovementStrategy
import lld_self.elevator.strategies.Stop
import java.util.TreeSet


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

    private val upStops = TreeSet<Int>()
    private val downStops = TreeSet<Int>()

    val pendingStopCount: Int
        get() = upStops.size + downStops.size

    init {
        addObserver(display)
    }

    fun addObserver(observer: ElevatorObserver) {
        observers.add(observer)
    }

    internal fun addStop(floor: Int) {
        val direction = if (floor > currentFloor) Direction.UP else Direction.DOWN
        addStop(floor, direction)
    }

    fun addStop(floor: Int, direction: Direction) {
        require(direction != Direction.IDLE) { "Stop direction must be UP or DOWN" }

        if (floor == currentFloor) {
            door.open()
            return
        }

        val stops = if (direction == Direction.UP) upStops else downStops
        stops.add(floor)
    }

    fun step() {
        door.step()

        if (door.state != DoorState.CLOSED) {
            return
        }

        val next = movementStrategy.nextStop(currentFloor, direction, upStops, downStops)

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

    private fun arrive(stop: Stop) {
        direction = stop.direction
        val stops = if (direction == Direction.UP) upStops else downStops
        stops.remove(stop.floor)
        state = ElevatorState.STOPPED
        door.open()
    }
}
