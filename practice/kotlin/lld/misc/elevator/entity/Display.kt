package lld.misc.elevator.entity

// =====================================================================
//  DISPLAY
// =====================================================================

class Display {
    var floor: Int = 0
        private set
    var direction: Direction = Direction.IDLE
        private set

    fun show(floor: Int, direction: Direction) {
        this.floor = floor
        this.direction = direction
    }
}
