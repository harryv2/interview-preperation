package lld_self.elevator.strategies

import lld_self.elevator.entities.Direction
import java.util.TreeSet
import kotlin.math.abs

interface ElevatorMovementStrategy {
    fun nextStop(currentFloor: Int, direction: Direction, stops: TreeSet<Int>): Int?
}


class LookMovementStrategy : ElevatorMovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        stops: TreeSet<Int>
    ): Int? {
        return when (direction) {
            Direction.UP -> {
                stops.ceiling(currentFloor) ?: stops.floor(currentFloor)
            }
            Direction.DOWN -> {
                stops.floor(currentFloor) ?: stops.ceiling(currentFloor)
            }
            Direction.IDLE -> {
                val above = stops.ceiling(currentFloor)
                val below = stops.floor(currentFloor)
                listOfNotNull(above, below).minByOrNull { abs(it - currentFloor) }
            }
        }
    }

}