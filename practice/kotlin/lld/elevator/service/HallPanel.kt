package lld.elevator.service

import lld.elevator.entity.Direction

// =====================================================================
//  HALL PANEL
//
//  A person in the hallway has not chosen a car yet, so the press goes
//  to the controller rather than to any one elevator. That is the whole
//  difference from ElevatorPanel, and the reason this one lives beside
//  the controller instead of beside the car.
// =====================================================================

class HallPanel(
    private val floorNumber: Int,
    private val controller: ElevatorController
) {
    fun pressUp() = controller.requestElevator(floorNumber, Direction.UP)
    fun pressDown() = controller.requestElevator(floorNumber, Direction.DOWN)

    fun isUpLit(): Boolean = controller.isRequestPending(floorNumber, Direction.UP)
    fun isDownLit(): Boolean = controller.isRequestPending(floorNumber, Direction.DOWN)
}
