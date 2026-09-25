package lld.games.chess.piece

import lld.games.chess.entity.Board
import lld.games.chess.entity.Color
import lld.games.chess.entity.Square


private val STRAIGHT = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
private val DIAGONAL = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
private val KNIGHT_JUMPS = listOf(2 to 1, 2 to -1, -2 to 1, -2 to -1, 1 to 2, 1 to -2, -1 to 2, -1 to -2)


class Rook(color: Color) : Piece(color, PieceType.ROOK) {
    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        return slide(board, from, STRAIGHT)
    }

    override fun copy(): Piece = Rook(color).also { it.hasMoved = hasMoved }
}


class Bishop(color: Color) : Piece(color, PieceType.BISHOP) {
    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        return slide(board, from, DIAGONAL)
    }

    override fun copy(): Piece = Bishop(color).also { it.hasMoved = hasMoved }
}


class Queen(color: Color) : Piece(color, PieceType.QUEEN) {
    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        return slide(board, from, STRAIGHT + DIAGONAL)
    }

    override fun copy(): Piece = Queen(color).also { it.hasMoved = hasMoved }
}


class Knight(color: Color) : Piece(color, PieceType.KNIGHT) {
    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        return step(board, from, KNIGHT_JUMPS)
    }

    override fun copy(): Piece = Knight(color).also { it.hasMoved = hasMoved }
}


class King(color: Color) : Piece(color, PieceType.KING) {
    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        return step(board, from, STRAIGHT + DIAGONAL)
    }

    override fun copy(): Piece = King(color).also { it.hasMoved = hasMoved }
}


class Pawn(color: Color) : Piece(color, PieceType.PAWN) {

    private val forward = if (color == Color.WHITE) 1 else -1
    private val startRow = if (color == Color.WHITE) 1 else 6

    override fun pseudoMoves(board: Board, from: Square): List<Square> {
        val moves = mutableListOf<Square>()

        val oneUp = from.offset(forward, 0)
        if (oneUp.isOnBoard() && board.pieceAt(oneUp) == null) {
            moves.add(oneUp)

            val twoUp = from.offset(forward * 2, 0)
            if (from.row == startRow && board.pieceAt(twoUp) == null) {
                moves.add(twoUp)
            }
        }

        attackedSquares(board, from).forEach {
            val target = board.pieceAt(it)
            if (target != null && target.color != color) {
                moves.add(it)
            }
        }

        return moves
    }

    override fun attackedSquares(board: Board, from: Square): List<Square> {
        return listOf(from.offset(forward, 1), from.offset(forward, -1)).filter { it.isOnBoard() }
    }

    fun promotionRow(): Int {
        return if (color == Color.WHITE) 7 else 0
    }

    override fun copy(): Piece = Pawn(color).also { it.hasMoved = hasMoved }
}
