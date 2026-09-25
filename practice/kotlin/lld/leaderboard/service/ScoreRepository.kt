package lld.leaderboard.service

import lld.leaderboard.entity.LeaderboardKey
import java.time.Instant
import java.util.TreeSet


class ScoreRecord(
    val key: LeaderboardKey,
    val playerId: String,
    val score: Long,
    val achievedAt: Instant
)


// the durable copy. Note what is deliberately missing: there is no rankOf here. A rank out of a relational
// table is a window function over the whole partition, which is exactly the query the ordered set exists to avoid.
interface ScoreRepository {

    fun save(record: ScoreRecord)

    fun find(key: LeaderboardKey, playerId: String): ScoreRecord?

    fun top(key: LeaderboardKey, n: Int): List<ScoreRecord>

    fun allFor(key: LeaderboardKey): List<ScoreRecord>
}


// stands in for the table, the two maps are shaped like the primary key and the covering index in the README
class InMemoryScoreRepository : ScoreRepository {

    private val byPrimaryKey = HashMap<LeaderboardKey, HashMap<String, ScoreRecord>>()
    private val byRankIndex = HashMap<LeaderboardKey, TreeSet<ScoreRecord>>()

    override fun save(record: ScoreRecord) {
        val rows = byPrimaryKey.getOrPut(record.key) { HashMap() }
        val index = byRankIndex.getOrPut(record.key) { TreeSet(INDEX_ORDER) }

        rows.put(record.playerId, record)?.let { index.remove(it) }
        index.add(record)
    }

    override fun find(key: LeaderboardKey, playerId: String): ScoreRecord? {
        return byPrimaryKey[key]?.get(playerId)
    }

    // an index range scan, the rows come back already in order so the database never sorts
    override fun top(key: LeaderboardKey, n: Int): List<ScoreRecord> {
        return byRankIndex[key].orEmpty().asSequence().take(n).toList()
    }

    override fun allFor(key: LeaderboardKey): List<ScoreRecord> {
        return byRankIndex[key].orEmpty().toList()
    }

    companion object {
        // (score DESC, achieved_at ASC, player_id) — the trailing columns are what make the order total
        private val INDEX_ORDER: Comparator<ScoreRecord> = compareByDescending<ScoreRecord> { it.score }
            .thenBy { it.achievedAt }
            .thenBy { it.playerId }
    }
}
