package lld_self.elevator.entities

class HallDisplay(
    val floorNumber: Int,
    elevators: List<Elevator>
) : ElevatorObserver {
    private val indicators = mutableMapOf<String, String>()

    init {
        elevators.forEach {
            indicators[it.name] = "${it.currentFloor} ${it.direction.symbol}"
        }
    }

    override fun onFloorChanged(elevator: Elevator, floor: Int) {
        indicators[elevator.name] = "$floor ${elevator.direction.symbol}"
    }

    override fun onDirectionChanged(elevator: Elevator, direction: Direction) {
        indicators[elevator.name] = "${elevator.currentFloor} ${direction.symbol}"
    }

    override fun onDoorStateChanged(elevator: Elevator, doorState: DoorState) {
        if (doorState == DoorState.OPEN && elevator.currentFloor == floorNumber) {
            println("      [floor $floorNumber] ding: ${elevator.name} going ${elevator.direction}")
        }
    }

    fun render(): String {
        return indicators.entries.joinToString(", ") { "${it.key}: ${it.value}" }
    }
}
