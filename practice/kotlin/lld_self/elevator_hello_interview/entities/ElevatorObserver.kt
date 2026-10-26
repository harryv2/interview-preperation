package lld_self.elevator_hello_interview.entities

interface ElevatorObserver {
    fun onFloorChanged(elevator: Elevator, floor: Int) {
    }

    fun onDirectionChanged(elevator: Elevator, direction: Direction) {
    }

    fun onDoorStateChanged(elevator: Elevator, doorState: DoorState) {
    }
}
