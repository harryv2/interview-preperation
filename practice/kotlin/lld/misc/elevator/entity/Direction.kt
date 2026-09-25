package lld.misc.elevator.entity

// =====================================================================
//  DIRECTION
//
//  Filter rule 2: a fixed set of labels with no data and no behaviour
//  is an enum, not a class.
// =====================================================================

enum class Direction {
    UP, DOWN, IDLE;

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        IDLE -> IDLE
    }
}
