package lld.misc.elevator.entity

// =====================================================================
//  ELEVATOR PANEL
//
//  The two button paths are different on purpose. A passenger inside
//  has already chosen their car, so the press goes straight to it.
//  A person in the hallway has not, so it goes to the controller --
//  see HallPanel.
//
//  Lives beside Elevator because the two point at each other: the car
//  owns its panel, the panel commands its car.
// =====================================================================

class ElevatorPanel(private val elevator: Elevator) {
    fun pressFloor(floor: Int) = elevator.addStop(floor)
    fun pressDoorOpen() = elevator.openDoor()
    fun pressDoorClose() = elevator.closeDoor()

    /** Computed, never stored. Two copies of one fact drift apart. */
    fun isFloorLit(floor: Int): Boolean = elevator.hasStop(floor)
}
