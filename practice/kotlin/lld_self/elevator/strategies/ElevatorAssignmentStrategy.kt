package lld_self.elevator.strategies

import lld_self.elevator.entities.Direction
import lld_self.elevator.entities.Elevator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface ElevatorAssignmentStrategy {
    fun pick(elevators: List<Elevator>, floor: Int, direction: Direction): Elevator
}


class RoundRobinAssignment : ElevatorAssignmentStrategy {
    private var next = 0

    override fun pick(
        elevators: List<Elevator>,
        floor: Int,
        direction: Direction
    ): Elevator {
        val elevator = elevators[next % elevators.size]
        next++
        return elevator
    }
}


class LeastLoadedAssignment : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        floor: Int,
        direction: Direction
    ): Elevator {
        return elevators.minBy { it.pendingStopCount }
    }
}


class AssignNearestInThatDirection : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        floor: Int,
        direction: Direction
    ): Elevator {
        val candidates = elevators
            .filter { canServeOnTheWay(it, floor, direction) }
            .ifEmpty { elevators }
        return candidates.minBy { abs(it.currentFloor - floor) }
    }

    private fun canServeOnTheWay(elevator: Elevator, floor: Int, direction: Direction): Boolean {
        return when (elevator.direction) {
            Direction.IDLE -> {
                true
            }
            Direction.UP -> {
                direction == Direction.UP && elevator.currentFloor <= floor
            }
            Direction.DOWN -> {
                direction == Direction.DOWN && elevator.currentFloor >= floor
            }
        }
    }
}

