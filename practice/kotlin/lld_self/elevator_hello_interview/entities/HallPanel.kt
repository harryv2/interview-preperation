package lld_self.elevator_hello_interview.entities

import lld_self.elevator_hello_interview.ElevatorController


class HallPanel(
    val floorNumber: Int,
    val controller: ElevatorController
) {

    fun callLift(direction: Direction) {
        controller.requestElevator(Request.pickup(floorNumber, direction))
    }
}
