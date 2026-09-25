package lld.games.snakeladder

import lld.games.snakeladder.entity.Board
import lld.games.snakeladder.entity.Game
import lld.games.snakeladder.entity.Jump
import lld.games.snakeladder.entity.Player
import lld.games.snakeladder.strategy.ScriptedDice
import lld.games.snakeladder.strategy.SingleDie

fun main() {

    val board = Board(
        size = 30,
        jumps = listOf(Jump(3, 22), Jump(8, 26), Jump(15, 25), Jump(20, 9), Jump(27, 5))
    )
    println(board)
    board.jumps().forEach { println("  $it") }

    val amit = Player("p1", "Amit")
    val bela = Player("p2", "Bela")

    println("\n== a scripted game ==")
    val game = Game(board, ScriptedDice(listOf(2, 6, 1, 5, 3, 4, 4, 2, 1)), listOf(amit, bela))
    while (game.winner == null) {
        println("  ${game.playTurn()}")
    }
    println("  ${game.winner} wins")

    println("\n== a random game ==")
    val random = Game(board, SingleDie(seed = 42), listOf(amit, bela))
    val won = random.play()
    println("  $won wins, Amit on ${random.positionOf(amit)}, Bela on ${random.positionOf(bela)}")

    println("\n== boards that are rejected ==")
    attempt("two jumps from one cell") { Board(10, listOf(Jump(4, 9), Jump(4, 2))) }
    attempt("a snake on the winning cell") { Board(10, listOf(Jump(10, 3))) }
    attempt("a jump on the starting cell") { Board(10, listOf(Jump(1, 7))) }
    attempt("a jump off the board") { Board(10, listOf(Jump(4, 44))) }
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
