package lld.games.chess.entity


data class Square(
    val row: Int,
    val col: Int
) {

    fun isOnBoard(): Boolean {
        return row in 0..7 && col in 0..7
    }

    fun offset(rowDelta: Int, colDelta: Int): Square {
        return Square(row + rowDelta, col + colDelta)
    }

    override fun toString(): String {
        return "${'a' + col}${row + 1}"
    }

    companion object {
        fun of(name: String): Square {
            require(name.length == 2) { "Bad square $name" }
            val col = name[0] - 'a'
            val row = name[1] - '1'
            val square = Square(row, col)
            require(square.isOnBoard()) { "Bad square $name" }
            return square
        }
    }
}


enum class Color {
    WHITE,
    BLACK;

    fun opponent(): Color {
        return if (this == WHITE) BLACK else WHITE
    }
}
