package lld.games.snakeladder.entity


// A snake and a ladder are the same thing: a jump from one cell to another. Two classes would mean two
// collections to search on every landing and every rule written twice.
class Jump(
    val from: Int,
    val to: Int
) {
    val isLadder: Boolean
        get() = to > from

    override fun toString(): String {
        return if (isLadder) "ladder $from->$to" else "snake $from->$to"
    }
}


class Board(
    val size: Int,
    jumps: List<Jump>
) {

    private val jumpsByStart = jumps.associateBy { it.from }

    init {
        require(size > 1) { "A board needs more than one cell" }
        require(jumpsByStart.size == jumps.size) { "Two jumps start on the same cell" }

        jumps.forEach {
            require(it.from in 2 until size) { "A jump cannot start on cell ${it.from}" }
            require(it.to in 1..size) { "Jump to ${it.to} is off the board" }
            require(it.from != it.to) { "A jump at ${it.from} goes nowhere" }
        }
    }

    fun jumpAt(cell: Int): Jump? {
        return jumpsByStart[cell]
    }

    fun jumps(): List<Jump> {
        return jumpsByStart.values.sortedBy { it.from }
    }

    override fun toString(): String {
        val ladders = jumpsByStart.values.count { it.isLadder }
        return "Board 1..$size, $ladders ladders, ${jumpsByStart.size - ladders} snakes"
    }
}
