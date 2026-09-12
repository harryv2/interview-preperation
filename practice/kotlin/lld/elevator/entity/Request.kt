package lld.elevator.entity

// =====================================================================
//  REQUEST
//
//  Immutable, so it works as a map key. The controller holds the
//  assignment in its own map rather than as a mutable field here --
//  a public var holding a reference to an Elevator would let anything
//  rewire it.
// =====================================================================

data class Request(val floor: Int, val direction: Direction) {
    override fun toString(): String = "$floor${if (direction == Direction.UP) "↑" else "↓"}"
}
