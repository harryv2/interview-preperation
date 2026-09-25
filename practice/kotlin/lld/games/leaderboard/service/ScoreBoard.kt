package lld.games.leaderboard.service

import lld.games.leaderboard.entity.LeaderboardKey
import lld.games.leaderboard.entity.RankedEntry
import lld.games.leaderboard.entity.ScoreEntry
import lld.games.leaderboard.strategy.ScorePolicy
import lld.games.leaderboard.structure.RankedSet
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// one board is a Redis ZSET: a hash from player to their entry, plus an ordered set of those entries.
// The hash is what makes a score update O(1) to find and O(log n) to move, instead of O(n) to hunt for.
class ScoreBoard(
    val key: LeaderboardKey,
    private val ordered: RankedSet<ScoreEntry>,
    private val policy: ScorePolicy
) {

    private val lock = ReentrantLock()
    private val byPlayer = HashMap<String, ScoreEntry>()

    val size: Int
        get() = byPlayer.size

    fun submit(playerId: String, score: Long, at: Instant): ScoreEntry {
        lock.withLock {
            val previous = byPlayer[playerId]
            val merged = policy.merge(previous?.score, score)

            // a personal best that was not beaten keeps its original timestamp, which is what breaks ties fairly
            if (previous != null && previous.score == merged) {
                return previous
            }

            return place(playerId, merged, at, previous)
        }
    }

    // cold start path, the stored score has already been through the policy so it goes straight in
    fun restore(playerId: String, score: Long, at: Instant): ScoreEntry {
        lock.withLock {
            return place(playerId, score, at, byPlayer[playerId])
        }
    }

    fun scoreOf(playerId: String): Long? {
        lock.withLock {
            return byPlayer[playerId]?.score
        }
    }

    fun rankOf(playerId: String): Int? {
        lock.withLock {
            val entry = byPlayer[playerId] ?: return null
            val rank = ordered.rankOf(entry)
            return if (rank < 0) null else rank + 1
        }
    }

    fun top(n: Int): List<RankedEntry> {
        lock.withLock {
            return ordered.range(0, n).mapIndexed { offset, entry -> RankedEntry(offset + 1, entry) }
        }
    }

    fun around(playerId: String, radius: Int): List<RankedEntry> {
        lock.withLock {
            val entry = byPlayer[playerId] ?: return emptyList()
            val rank = ordered.rankOf(entry)
            if (rank < 0) {
                return emptyList()
            }

            val from = maxOf(0, rank - radius)
            return ordered.range(from, radius * 2 + 1)
                .mapIndexed { offset, found -> RankedEntry(from + offset + 1, found) }
        }
    }

    private fun place(playerId: String, score: Long, at: Instant, previous: ScoreEntry?): ScoreEntry {
        if (previous != null) {
            ordered.remove(previous)
        }

        val entry = ScoreEntry(playerId, score, at)
        byPlayer[playerId] = entry
        ordered.insert(entry)
        return entry
    }

    override fun toString(): String {
        return "$key ($size players)"
    }
}
