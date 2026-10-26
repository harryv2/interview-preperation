package lld_self.elevator_hello_interview.entities

class ElevatorPanel(
    val elevator: Elevator
) {
    fun pressFloor(floor: Int) {
        elevator.addRequest(Request.destination(floor))
    }
}
