package lld.snakeladder.entity

import lld.snakeladder.strategy.Dice


class Player(val id: String, val name: String) {
    override fun toString(): String = name
}


class Move(
    val player: Player,
    val roll: Int,
    val from: Int,
    val to: Int,
    val jump: Jump?,
    val rollsAgain: Boolean
) {
    override fun toString(): String {
        if (from == to) {
            return "$player rolled $roll, too big to finish, stays on $from"
        }

        val via = if (jump == null) "" else " via $jump"
        val again = if (rollsAgain) ", rolls again" else ""
        return "$player rolled $roll: $from -> $to$via$again"
    }
}


class Game(
    private val board: Board,
    private val dice: Dice,
    players: List<Player>
) {

    private val turnOrder = ArrayDeque(players)
    private val positions = players.associateTo(HashMap()) { it.id to START }

    var winner: Player? = null
        private set

    init {
        require(players.size >= 2) { "A game needs at least two players" }
    }

    fun positionOf(player: Player): Int {
        return positions.getValue(player.id)
    }

    fun playTurn(): Move {
        check(winner == null) { "$winner already won" }

        val player = turnOrder.first()
        val from = positions.getValue(player.id)
        val roll = dice.roll()
        val rollsAgain = roll == ROLL_AGAIN_ON

        // exact finish: a roll that would overshoot the last cell does not move the token
        if (from + roll > board.size) {
            return endTurn(Move(player, roll, from, from, null, rollsAgain))
        }

        val landed = from + roll
        val jump = board.jumpAt(landed)
        val to = jump?.to ?: landed
        positions[player.id] = to

        if (to == board.size) {
            winner = player
            return Move(player, roll, from, to, jump, false)
        }

        return endTurn(Move(player, roll, from, to, jump, rollsAgain))
    }

    fun play(maxTurns: Int = 1000): Player? {
        var turns = 0
        while (winner == null && turns < maxTurns) {
            playTurn()
            turns++
        }
        return winner
    }

    // the queue only rotates when the turn actually ends, which is all "roll again on a six" needs to be
    private fun endTurn(move: Move): Move {
        if (!move.rollsAgain) {
            turnOrder.addLast(turnOrder.removeFirst())
        }
        return move
    }

    companion object {
        const val START = 1
        const val ROLL_AGAIN_ON = 6
    }
}
