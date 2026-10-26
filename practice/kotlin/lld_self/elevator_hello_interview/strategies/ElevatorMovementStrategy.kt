package lld_self.elevator_hello_interview.strategies

import lld_self.elevator_hello_interview.entities.Direction
import lld_self.elevator_hello_interview.entities.Request
import lld_self.elevator_hello_interview.entities.RequestType
import kotlin.math.abs

data class Stop(
    val floor: Int,
    val direction: Direction
)

interface ElevatorMovementStrategy {
    fun nextStop(currentFloor: Int, direction: Direction, requests: Set<Request>): Stop?
}


class LookMovementStrategy : ElevatorMovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        requests: Set<Request>
    ): Stop? {
        return when (direction) {
            Direction.UP -> {
                nextAbove(currentFloor, requests) ?: nextBelow(currentFloor, requests)
            }
            Direction.DOWN -> {
                nextBelow(currentFloor, requests) ?: nextAbove(currentFloor, requests)
            }
            Direction.IDLE -> {
                val above = nextAbove(currentFloor, requests)
                val below = nextBelow(currentFloor, requests)
                listOfNotNull(above, below).minByOrNull { abs(it.floor - currentFloor) }
            }
        }
    }

    // sweeping up stops only for up-calls and destinations; down-calls are skipped,
    // unless none of those remain, then ride to the highest down-call and reverse there
    private fun nextAbove(currentFloor: Int, requests: Set<Request>): Stop? {
        val stopFloor = requests
            .filter { it.floor >= currentFloor && it.servesDirection(Direction.UP) }
            .minOfOrNull { it.floor }
        if (stopFloor != null) {
            return stopWhileSweeping(stopFloor, Direction.UP, requests)
        }

        val turnFloor = requests
            .filter { it.floor >= currentFloor && it.type == RequestType.PICKUP_DOWN }
            .maxOfOrNull { it.floor }
        if (turnFloor != null) {
            return Stop(turnFloor, Direction.DOWN)
        }

        return null
    }

    private fun nextBelow(currentFloor: Int, requests: Set<Request>): Stop? {
        val stopFloor = requests
            .filter { it.floor <= currentFloor && it.servesDirection(Direction.DOWN) }
            .maxOfOrNull { it.floor }
        if (stopFloor != null) {
            return stopWhileSweeping(stopFloor, Direction.DOWN, requests)
        }

        val turnFloor = requests
            .filter { it.floor <= currentFloor && it.type == RequestType.PICKUP_UP }
            .minOfOrNull { it.floor }
        if (turnFloor != null) {
            return Stop(turnFloor, Direction.UP)
        }

        return null
    }

    // keep the sweep direction if someone here wants to go that way or more stops lie ahead,
    // otherwise this floor is the turning point
    private fun stopWhileSweeping(floor: Int, sweep: Direction, requests: Set<Request>): Stop {
        val pickupHere = requests.any { it.floor == floor && it.direction == sweep }
        val moreAhead = requests.any { if (sweep == Direction.UP) it.floor > floor else it.floor < floor }
        if (pickupHere || moreAhead) {
            return Stop(floor, sweep)
        }

        val leftAfterStop = requests.filterNot { it.floor == floor && it.servesDirection(sweep) }
        if (leftAfterStop.isEmpty()) {
            return Stop(floor, Direction.IDLE)
        }

        return Stop(floor, if (sweep == Direction.UP) Direction.DOWN else Direction.UP)
    }
}
