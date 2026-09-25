package lld.misc.elevator.scheduler

import lld.misc.elevator.entity.Elevator
import lld.misc.elevator.entity.Request

/**
 * Stateful, so it cannot be shared between controllers. A stateless
 * strategy can be one shared instance; a stateful one needs its own.
 */
class RoundRobinStrategy : SchedulingStrategy {
    private var cursor = 0

    override fun selectElevator(elevators: List<Elevator>, request: Request): Elevator? {
        val usable = elevators.filter { it.isAvailable() }
        if (usable.isEmpty()) return null
        val chosen = usable[cursor % usable.size]
        cursor = (cursor + 1) % usable.size
        return chosen
    }
}
