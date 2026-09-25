package lld.games.chess.entity

import lld.games.chess.piece.Piece
import lld.games.chess.piece.PieceType


data class Move(
    val from: Square,
    val to: Square,
    val piece: PieceType,
    val color: Color,
    val captured: PieceType?,
    val promotedTo: PieceType?
) {

    override fun toString(): String {
        val take = if (captured == null) "-" else "x"
        val promo = promotedTo?.let { "=$it" } ?: ""
        return "${piece.symbol}$from$take$to$promo"
    }
}


enum class GameStatus {
    ACTIVE,
    CHECK,
    CHECKMATE,
    STALEMATE
}
