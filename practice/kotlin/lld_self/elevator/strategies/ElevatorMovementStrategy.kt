package lld_self.elevator.strategies

import lld_self.elevator.entities.Direction
import java.util.TreeSet
import kotlin.math.abs

interface ElevatorMovementStrategy {
    fun nextStop(currentFloor: Int, direction: Direction, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Int?
}


class LookMovementStrategy : ElevatorMovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        upStops: TreeSet<Int>,
        downStops: TreeSet<Int>
    ): Int? {
        return when (direction) {
            Direction.UP -> {
                nextAbove(currentFloor, upStops, downStops) ?: nextBelow(currentFloor, upStops, downStops)
            }
            Direction.DOWN -> {
                nextBelow(currentFloor, upStops, downStops) ?: nextAbove(currentFloor, upStops, downStops)
            }
            Direction.IDLE -> {
                val above = nextAbove(currentFloor, upStops, downStops)
                val below = nextBelow(currentFloor, upStops, downStops)
                listOfNotNull(above, below).minByOrNull { abs(it - currentFloor) }
            }
        }
    }

    // sweeping up serves up-calls in order; once none remain, ride to the highest down-call and reverse there
    private fun nextAbove(currentFloor: Int, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Int? {
        upStops.ceiling(currentFloor)?.let { return it }

        if (downStops.ceiling(currentFloor) != null) {
            return downStops.last()
        }

        return null
    }

    private fun nextBelow(currentFloor: Int, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Int? {
        downStops.floor(currentFloor)?.let { return it }

        if (upStops.floor(currentFloor) != null) {
            return upStops.first()
        }

        return null
    }
}
