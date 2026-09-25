package lld.chess.entity

import lld.chess.piece.Bishop
import lld.chess.piece.King
import lld.chess.piece.Knight
import lld.chess.piece.Pawn
import lld.chess.piece.Piece
import lld.chess.piece.PieceType
import lld.chess.piece.Queen
import lld.chess.piece.Rook


class Board {

    private val squares = Array(8) { arrayOfNulls<Piece>(8) }

    fun pieceAt(square: Square): Piece? {
        if (!square.isOnBoard()) {
            return null
        }
        return squares[square.row][square.col]
    }

    fun place(square: Square, piece: Piece?) {
        squares[square.row][square.col] = piece
    }

    fun squaresOf(color: Color): List<Square> {
        val found = mutableListOf<Square>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = squares[row][col]
                if (piece != null && piece.color == color) {
                    found.add(Square(row, col))
                }
            }
        }
        return found
    }

    fun kingSquare(color: Color): Square? {
        return squaresOf(color).firstOrNull { pieceAt(it)?.type == PieceType.KING }
    }

    fun isAttacked(square: Square, by: Color): Boolean {
        return squaresOf(by).any { from ->
            square in pieceAt(from)!!.attackedSquares(this, from)
        }
    }

    fun isInCheck(color: Color): Boolean {
        val king = kingSquare(color) ?: return false
        return isAttacked(king, color.opponent())
    }

    fun copy(): Board {
        val clone = Board()
        for (row in 0..7) {
            for (col in 0..7) {
                clone.squares[row][col] = squares[row][col]?.copy()
            }
        }
        return clone
    }

    override fun toString(): String {
        val out = StringBuilder()
        for (row in 7 downTo 0) {
            out.append(row + 1).append(" ")
            for (col in 0..7) {
                out.append(squares[row][col]?.toString() ?: ".").append(" ")
            }
            out.append("\n")
        }
        out.append("  a b c d e f g h")
        return out.toString()
    }

    companion object {
        fun standard(): Board {
            val board = Board()

            val backRank = listOf(
                ::Rook, ::Knight, ::Bishop, ::Queen, ::King, ::Bishop, ::Knight, ::Rook
            )

            backRank.forEachIndexed { col, create ->
                board.place(Square(0, col), create(Color.WHITE))
                board.place(Square(7, col), create(Color.BLACK))
            }

            for (col in 0..7) {
                board.place(Square(1, col), Pawn(Color.WHITE))
                board.place(Square(6, col), Pawn(Color.BLACK))
            }

            return board
        }
    }
}
