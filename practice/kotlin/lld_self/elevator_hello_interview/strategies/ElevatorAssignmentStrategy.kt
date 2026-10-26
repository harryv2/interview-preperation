package lld_self.elevator_hello_interview.strategies

import lld_self.elevator_hello_interview.entities.Direction
import lld_self.elevator_hello_interview.entities.Elevator
import lld_self.elevator_hello_interview.entities.Request
import kotlin.math.abs

interface ElevatorAssignmentStrategy {
    fun pick(elevators: List<Elevator>, request: Request): Elevator
}


class RoundRobinAssignment : ElevatorAssignmentStrategy {
    private var next = 0

    override fun pick(
        elevators: List<Elevator>,
        request: Request
    ): Elevator {
        val elevator = elevators[next % elevators.size]
        next++
        return elevator
    }
}


class LeastLoadedAssignment : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        request: Request
    ): Elevator {
        return elevators.minBy { it.pendingRequests.size }
    }
}


class AssignNearestInThatDirection : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        request: Request
    ): Elevator {
        val candidates = elevators
            .filter { canServeOnTheWay(it, request) }
            .ifEmpty { elevators }
        return candidates.minBy { abs(it.currentFloor - request.floor) }
    }

    private fun canServeOnTheWay(elevator: Elevator, request: Request): Boolean {
        return when (elevator.direction) {
            Direction.IDLE -> {
                true
            }
            Direction.UP -> {
                request.direction == Direction.UP && elevator.currentFloor <= request.floor
            }
            Direction.DOWN -> {
                request.direction == Direction.DOWN && elevator.currentFloor >= request.floor
            }
        }
    }
}


// hello interview dispatch: committed elevator first, then nearest idle, then nearest overall
class CommittedIdleNearestAssignment : ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        request: Request
    ): Elevator {
        val committed = elevators.filter { isCommittedPast(it, request) }
        if (committed.isNotEmpty()) {
            return nearest(committed, request)
        }

        val idle = elevators.filter { it.direction == Direction.IDLE }
        if (idle.isNotEmpty()) {
            return nearest(idle, request)
        }

        return nearest(elevators, request)
    }

    // moving the requested way, has not passed the floor yet, and already has a stop at or beyond it,
    // so it will not turn around before reaching the caller
    private fun isCommittedPast(elevator: Elevator, request: Request): Boolean {
        if (elevator.direction != request.direction) {
            return false
        }

        return when (request.direction) {
            Direction.UP -> {
                elevator.currentFloor <= request.floor &&
                    elevator.pendingRequests.any { it.floor >= request.floor }
            }
            Direction.DOWN -> {
                elevator.currentFloor >= request.floor &&
                    elevator.pendingRequests.any { it.floor <= request.floor }
            }
            Direction.IDLE -> {
                false
            }
        }
    }

    private fun nearest(elevators: List<Elevator>, request: Request): Elevator {
        return elevators.minBy { abs(it.currentFloor - request.floor) }
    }
}
