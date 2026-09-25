package lld.misc.elevator.scheduler

import lld.misc.elevator.entity.Elevator
import lld.misc.elevator.entity.Request

// =====================================================================
//  SCHEDULING STRATEGY  --  WHICH car answers a hall call?
//
//  Every one of these reads only public queries off Elevator. No
//  scheduling logic lives inside Elevator, which is why swapping the
//  strategy touches nothing else.
// =====================================================================

fun interface SchedulingStrategy {
    fun selectElevator(elevators: List<Elevator>, request: Request): Elevator?
}
