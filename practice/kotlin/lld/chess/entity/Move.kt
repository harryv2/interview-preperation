package lld.chess.entity

import lld.chess.piece.Piece
import lld.chess.piece.PieceType


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
