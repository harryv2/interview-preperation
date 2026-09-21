package lld.musicplayer.strategies

import kotlin.random.Random

class ShuffleOrder(private val random: Random = Random.Default) : PlayOrder {
    private var order = listOf<Int>()

    override fun reset(size: Int) {
        order = (0 until size).shuffled(random)
    }

    override fun songIndex(position: Int): Int {
        return order[position]
    }
}
