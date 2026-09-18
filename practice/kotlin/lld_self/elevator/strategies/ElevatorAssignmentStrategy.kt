package lld_self.elevator.strategies

import lld_self.elevator.entities.Direction
import lld_self.elevator.entities.Elevator
import kotlin.math.abs

interface ElevatorAssignmentStrategy {
    fun pick(elevators: List<Elevator>, floor: Int, direction: Direction): Elevator
}


class AssignNearestInThatDirection: ElevatorAssignmentStrategy {
    override fun pick(
        elevators: List<Elevator>,
        floor: Int,
        direction: Direction
    ): Elevator {
        val candidates = elevators
            .filter { it.direction == Direction.IDLE || it.direction == direction }
            .ifEmpty { elevators }
        return candidates.minBy { abs(it.currentFloor - floor) }
    }

}