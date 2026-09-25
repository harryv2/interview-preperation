package lld.games.chess

import lld.games.chess.entity.Color
import lld.games.chess.entity.GameStatus
import lld.games.chess.entity.Square
import lld.games.chess.service.Game

fun main() {

    banner("fool's mate, the fastest checkmate there is")
    val game = Game()
    listOf("f2" to "f3", "e7" to "e5", "g2" to "g4", "d8" to "h4").forEach { (from, to) ->
        val move = game.play(Square.of(from), Square.of(to))
        println("  $move   ${game.status}")
    }
    println()
    println(game.board)
    println("  winner: ${game.winner}, moves played: ${game.history.size}")

    banner("legal moves are filtered by check")
    val pinned = Game()
    listOf("e2" to "e4", "e7" to "e5", "f1" to "c4", "b8" to "c6", "d1" to "h5", "g8" to "f6").forEach { (from, to) ->
        pinned.play(Square.of(from), Square.of(to))
    }
    println("  black just played Nf6, white plays Qxf7#")
    val mate = pinned.play(Square.of("h5"), Square.of("f7"))
    println("  $mate   ${pinned.status}, winner ${pinned.winner}")

    banner("a king may not walk into check")
    val safety = Game()
    listOf("e2" to "e4", "e7" to "e5").forEach { (f, t) -> safety.play(Square.of(f), Square.of(t)) }
    println("  white king on e1 can go to: ${safety.legalMovesFrom(Square.of("e1"))}")

    banner("rejected")
    val g = Game()
    attempt("move a piece that is not yours") {
        g.play(Square.of("e7"), Square.of("e5"))
    }
    attempt("move from an empty square") {
        g.play(Square.of("e4"), Square.of("e5"))
    }
    attempt("a knight moving like a rook") {
        g.play(Square.of("b1"), Square.of("b3"))
    }
    attempt("a pawn jumping three") {
        g.play(Square.of("e2"), Square.of("e5"))
    }
    attempt("play on after checkmate") {
        game.play(Square.of("a2"), Square.of("a3"))
    }
    attempt("a square off the board") {
        Square.of("j9")
    }

    banner("opening move count")
    println("  white has ${Game().allLegalMoves(Color.WHITE).size} legal opening moves (should be 20)")
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
