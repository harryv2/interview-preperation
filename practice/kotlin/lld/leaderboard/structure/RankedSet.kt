package lld.leaderboard.structure


// the ordered half of a Redis ZSET, the hash half lives on ScoreBoard
interface RankedSet<T> {

    val size: Int

    fun insert(value: T)

    fun remove(value: T): Boolean

    // 0 based position in the ordering, -1 when the value is not present
    fun rankOf(value: T): Int

    fun select(index: Int): T?

    fun range(from: Int, count: Int): List<T>
}
