package lld.games.snakeladder.strategy

import kotlin.random.Random


// The one thing that genuinely varies, and the one seam worth having: a game that can only roll randomly
// cannot be demonstrated or tested, only watched.
interface Dice {
    fun roll(): Int
}


class SingleDie(seed: Int? = null) : Dice {

    private val random = if (seed == null) Random.Default else Random(seed)

    override fun roll(): Int {
        return random.nextInt(1, 7)
    }
}


class ScriptedDice(private val rolls: List<Int>) : Dice {

    private var next = 0

    override fun roll(): Int {
        check(next < rolls.size) { "The script ran out after ${rolls.size} rolls" }
        return rolls[next++]
    }
}
