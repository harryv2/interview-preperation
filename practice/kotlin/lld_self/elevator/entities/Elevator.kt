package lld_self.elevator.entities

import lld_self.elevator.strategies.ElevatorMovementStrategy
import java.util.TreeSet


enum class ElevatorState {
    STOPPED,
    MOVING
}

enum class Direction {
    UP,
    DOWN,
    IDLE
}

class Elevator(
    val name: String,
    val movementStrategy: ElevatorMovementStrategy
) {
    val door = Door()

    val elevatorPanel = ElevatorPanel(this)

    var direction: Direction = Direction.IDLE
        private set
    var currentFloor: Int = 0
        private set
    var state: ElevatorState = ElevatorState.STOPPED
        private set

    private val stops = TreeSet<Int>()


    fun addStop(floor: Int) {
        if (floor == currentFloor) {
            door.open()
            return
        }
        stops.add(floor)
    }

    fun step() {
        door.step()

        if (door.state != DoorState.CLOSED) {
            return
        }

        val nextStop = movementStrategy.nextStop(currentFloor, direction, stops)


        if (nextStop == null) {
            direction = Direction.IDLE
            state = ElevatorState.STOPPED
            return
        }

        if (nextStop == currentFloor) {
            stops.remove(nextStop)
            state = ElevatorState.STOPPED
            door.open()
            return
        }

        direction = if(nextStop > currentFloor) Direction.UP else Direction.DOWN
        state = ElevatorState.MOVING
        currentFloor += if (direction == Direction.UP) 1 else -1
    }
}