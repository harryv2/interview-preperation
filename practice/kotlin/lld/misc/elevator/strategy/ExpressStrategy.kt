package lld.misc.elevator.strategy

import lld.misc.elevator.entity.Direction
import java.util.NavigableSet
import java.util.TreeSet

/**
 * A freight car that never stops at certain floors. Decorates another
 * strategy rather than reimplementing the direction logic.
 */
class ExpressStrategy(
    private val skippedFloors: Set<Int>,
    private val base: MovementStrategy = LookStrategy()
) : MovementStrategy {
    override fun nextStop(
        currentFloor: Int,
        direction: Direction,
        stops: NavigableSet<Int>
    ): Int? {
        val allowed = TreeSet(stops.filterNot { it in skippedFloors })
        return base.nextStop(currentFloor, direction, allowed)
    }
}
