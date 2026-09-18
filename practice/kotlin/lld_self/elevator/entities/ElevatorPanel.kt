package lld_self.elevator.entities

class ElevatorPanel(
    val elevator: Elevator
) {
    fun pressFloor(floor: Int) {
        elevator.addStop(floor)
    }
}