package lld.elevator.service

import lld.elevator.entity.Elevator
import lld.elevator.scheduler.NearestCarStrategy
import lld.elevator.scheduler.SchedulingStrategy

// =====================================================================
//  BUILDING
// =====================================================================

class Building(
    floorCount: Int,
    elevatorCount: Int,
    strategy: SchedulingStrategy = NearestCarStrategy()
) {
    val elevators: List<Elevator> = (1..elevatorCount).map { Elevator(id = it) }
    val controller: ElevatorController = ElevatorController(elevators, strategy)
    val floors: List<Floor> = (0 until floorCount).map { Floor(it, controller) }

    fun floor(number: Int): Floor = floors[number]

    fun step() = controller.step()
}
