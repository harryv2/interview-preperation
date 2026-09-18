package lld_self.elevator.entities

import lld_self.elevator.ElevatorController


class HallPanel(
    val floorNumber: Int,
    val controller: ElevatorController
) {

    fun callLift(direction: Direction) {
        controller.handleHallRequest(floorNumber, direction)
    }
}