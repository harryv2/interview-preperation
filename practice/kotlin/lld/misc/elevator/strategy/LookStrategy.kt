package lld.misc.elevator.strategy

import lld.misc.elevator.entity.Direction
import java.util.NavigableSet

/** Keep going the current way until nothing is left ahead, then turn around. */
class LookStrategy : MovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        stops: NavigableSet<Int>
    ): Int? = when (direction) {
        Direction.UP -> stops.ceiling(currentFloor)   // smallest stop >= here
        Direction.DOWN -> stops.floor(currentFloor)   // largest stop <= here
        Direction.IDLE -> null
    }
}
