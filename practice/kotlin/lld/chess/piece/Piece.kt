package lld.chess.piece

import lld.chess.entity.Board
import lld.chess.entity.Color
import lld.chess.entity.Square


enum class PieceType(val symbol: String) {
    KING("K"),
    QUEEN("Q"),
    ROOK("R"),
    BISHOP("B"),
    KNIGHT("N"),
    PAWN("P")
}


abstract class Piece(
    val color: Color,
    val type: PieceType
) {

    var hasMoved: Boolean = false

    abstract fun pseudoMoves(board: Board, from: Square): List<Square>

    open fun attackedSquares(board: Board, from: Square): List<Square> {
        return pseudoMoves(board, from)
    }

    protected fun slide(board: Board, from: Square, directions: List<Pair<Int, Int>>): List<Square> {
        val moves = mutableListOf<Square>()

        directions.forEach { (rowDelta, colDelta) ->
            var next = from.offset(rowDelta, colDelta)
            while (next.isOnBoard()) {
                val blocker = board.pieceAt(next)
                if (blocker == null) {
                    moves.add(next)
                } else {
                    if (blocker.color != color) {
                        moves.add(next)
                    }
                    break
                }
                next = next.offset(rowDelta, colDelta)
            }
        }

        return moves
    }

    protected fun step(board: Board, from: Square, offsets: List<Pair<Int, Int>>): List<Square> {
        return offsets
            .map { from.offset(it.first, it.second) }
            .filter { it.isOnBoard() && board.pieceAt(it)?.color != color }
    }

    abstract fun copy(): Piece

    override fun toString(): String {
        return if (color == Color.WHITE) type.symbol else type.symbol.lowercase()
    }
}
