package lld.games.chess.service

import lld.games.chess.entity.Board
import lld.games.chess.entity.Color
import lld.games.chess.entity.GameStatus
import lld.games.chess.entity.Move
import lld.games.chess.entity.Square
import lld.games.chess.piece.Pawn
import lld.games.chess.piece.Piece
import lld.games.chess.piece.PieceType
import lld.games.chess.piece.Queen


class Game(
    val board: Board = Board.standard()
) {

    var turn: Color = Color.WHITE
        private set

    var status: GameStatus = GameStatus.ACTIVE
        private set

    val history = mutableListOf<Move>()

    var winner: Color? = null
        private set

    fun legalMovesFrom(from: Square): List<Square> {
        val piece = board.pieceAt(from) ?: return emptyList()
        if (piece.color != turn) {
            return emptyList()
        }
        return piece.pseudoMoves(board, from).filter { leavesKingSafe(from, it) }
    }

    fun allLegalMoves(color: Color): List<Pair<Square, Square>> {
        return board.squaresOf(color).flatMap { from ->
            val piece = board.pieceAt(from)!!
            piece.pseudoMoves(board, from)
                .filter { leavesKingSafe(from, it) }
                .map { from to it }
        }
    }

    fun play(from: Square, to: Square): Move {
        check(status == GameStatus.ACTIVE || status == GameStatus.CHECK) { "Game is over: $status" }

        val piece = board.pieceAt(from)
        requireNotNull(piece) { "No piece on $from" }
        require(piece.color == turn) { "It is ${turn}'s turn, $from holds a ${piece.color} piece" }
        require(to in piece.pseudoMoves(board, from)) { "${piece.type} can not move $from to $to" }
        require(leavesKingSafe(from, to)) { "That move leaves the $turn king in check" }

        val captured = board.pieceAt(to)?.type
        val promotion = applyMove(board, from, to)

        val move = Move(from, to, piece.type, piece.color, captured, promotion)
        history.add(move)

        turn = turn.opponent()
        status = statusFor(turn)
        if (status == GameStatus.CHECKMATE) {
            winner = turn.opponent()
        }

        return move
    }

    private fun statusFor(color: Color): GameStatus {
        val inCheck = board.isInCheck(color)
        val hasMoves = allLegalMoves(color).isNotEmpty()

        if (inCheck && !hasMoves) {
            return GameStatus.CHECKMATE
        }
        if (!inCheck && !hasMoves) {
            return GameStatus.STALEMATE
        }
        if (inCheck) {
            return GameStatus.CHECK
        }
        return GameStatus.ACTIVE
    }

    private fun leavesKingSafe(from: Square, to: Square): Boolean {
        val piece = board.pieceAt(from) ?: return false
        val trial = board.copy()
        applyMove(trial, from, to)
        return !trial.isInCheck(piece.color)
    }

    private fun applyMove(target: Board, from: Square, to: Square): PieceType? {
        val piece = target.pieceAt(from)!!
        target.place(from, null)
        piece.hasMoved = true

        if (piece is Pawn && to.row == piece.promotionRow()) {
            target.place(to, Queen(piece.color))
            return PieceType.QUEEN
        }

        target.place(to, piece)
        return null
    }
}
