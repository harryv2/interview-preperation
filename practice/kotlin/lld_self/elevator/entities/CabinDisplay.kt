package lld_self.elevator.entities

class CabinDisplay : ElevatorObserver {
    private var floor = 0
    private var direction = Direction.IDLE

    val text: String
        get() = "$floor ${direction.symbol}"

    override fun onFloorChanged(elevator: Elevator, floor: Int) {
        this.floor = floor
    }

    override fun onDirectionChanged(elevator: Elevator, direction: Direction) {
        this.direction = direction
    }
}
