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


class EstimatedArrivalAssignment(
    private val ticksPerStop: Int = 16
) : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        floor: Int,
        direction: Direction
    ): Elevator {
        return elevators.minBy { estimateTicks(it, floor, direction) }
    }

    // on the way: straight distance; otherwise ride to the end of the current sweep and come back
    private fun estimateTicks(elevator: Elevator, floor: Int, direction: Direction): Int {
        val current = elevator.currentFloor

        val travel = when (elevator.direction) {
            Direction.IDLE -> {
                abs(current - floor)
            }
            Direction.UP -> {
                if (direction == Direction.UP && current <= floor) {
                    floor - current
                } else {
                    val turn = max(elevator.highestPendingStop() ?: current, floor)
                    (turn - current) + (turn - floor)
                }
            }
            Direction.DOWN -> {
                if (direction == Direction.DOWN && current >= floor) {
                    current - floor
                } else {
                    val turn = min(elevator.lowestPendingStop() ?: current, floor)
                    (current - turn) + (floor - turn)
                }
            }
        }

        return travel + elevator.pendingStopCount * ticksPerStop
    }
}
