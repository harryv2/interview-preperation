package lld_self.elevator.strategies

import lld_self.elevator.entities.Direction
import java.util.TreeSet
import kotlin.math.abs

data class Stop(
    val floor: Int,
    val direction: Direction
)

interface ElevatorMovementStrategy {
    fun nextStop(currentFloor: Int, direction: Direction, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Stop?
}


class LookMovementStrategy : ElevatorMovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        upStops: TreeSet<Int>,
        downStops: TreeSet<Int>
    ): Stop? {
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
                listOfNotNull(above, below).minByOrNull { abs(it.floor - currentFloor) }
            }
        }
    }

    // sweeping up serves up-calls in order; once none remain, ride to the highest down-call and reverse there
    private fun nextAbove(currentFloor: Int, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Stop? {
        upStops.ceiling(currentFloor)?.let { return Stop(it, Direction.UP) }

        if (downStops.ceiling(currentFloor) != null) {
            return Stop(downStops.last(), Direction.DOWN)
        }

        return null
    }

    private fun nextBelow(currentFloor: Int, upStops: TreeSet<Int>, downStops: TreeSet<Int>): Stop? {
        downStops.floor(currentFloor)?.let { return Stop(it, Direction.DOWN) }

        if (upStops.floor(currentFloor) != null) {
            return Stop(upStops.first(), Direction.UP)
        }

        return null
    }
}
