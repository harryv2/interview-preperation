package lld.misc.elevator.scheduler

import lld.misc.elevator.entity.Elevator
import lld.misc.elevator.entity.Request

/** Spread the load by pending work rather than by distance. Stateless. */
class LeastBusyStrategy : SchedulingStrategy {
    override fun selectElevator(elevators: List<Elevator>, request: Request): Elevator? =
        elevators
            .filter { it.isAvailable() }
            .minWithOrNull(
                compareBy({ it.pendingStopCount() }, { it.id })   // tie-break, always
            )
}
