package lld.misc.elevator.scheduler

import lld.misc.elevator.entity.Direction
import lld.misc.elevator.entity.Elevator
import lld.misc.elevator.entity.Request

/** Closest car wins, with a penalty for cars that would have to turn around. */
class NearestCarStrategy : SchedulingStrategy {

    companion object {
        const val WRONG_WAY_PENALTY = 100
    }

    override fun selectElevator(elevators: List<Elevator>, request: Request): Elevator? =
        elevators
            .filter { it.isAvailable() }
            .minByOrNull { score(it, request) }

    private fun score(elevator: Elevator, request: Request): Int {
        val distance = Math.abs(elevator.currentFloor() - request.floor)

        // Parked. Distance is the whole story.
        if (elevator.direction() == Direction.IDLE) return distance

        val requestIsAhead = when (elevator.direction()) {
            Direction.UP -> request.floor >= elevator.currentFloor()
            Direction.DOWN -> request.floor <= elevator.currentFloor()
            Direction.IDLE -> true
        }
        val goingSameWay = elevator.direction() == request.direction

        return if (requestIsAhead && goingSameWay) distance
        else distance + WRONG_WAY_PENALTY
    }
}
