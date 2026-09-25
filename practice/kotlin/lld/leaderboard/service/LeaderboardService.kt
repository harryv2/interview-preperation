package lld.leaderboard.service

import lld.leaderboard.entity.LeaderboardKey
import lld.leaderboard.entity.Period
import lld.leaderboard.entity.RankedEntry
import lld.leaderboard.entity.ScoreEntry
import lld.leaderboard.strategy.ScorePolicy
import lld.leaderboard.structure.OrderStatisticTree
import lld.leaderboard.structure.RankedSet
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class LeaderboardService(
    private val repository: ScoreRepository,
    private val policy: ScorePolicy,
    private val periods: List<Period> = listOf(Period.DAILY, Period.WEEKLY, Period.ALL_TIME),
    private val newRankedSet: () -> RankedSet<ScoreEntry> = { OrderStatisticTree(ScoreEntry.ORDER) }
) {

    private val lock = ReentrantLock()
    private val boards = HashMap<LeaderboardKey, ScoreBoard>()

    // one submission fans out into every window the player is competing in, each board is independent
    fun submit(gameId: String, playerId: String, score: Long, at: Instant = Instant.now()): List<ScoreEntry> {
        return periods.map {
            val key = LeaderboardKey.of(gameId, it, at)
            val entry = board(key).submit(playerId, score, at)
            repository.save(ScoreRecord(key, playerId, entry.score, entry.achievedAt))
            entry
        }
    }

    fun rankOf(gameId: String, playerId: String, period: Period, at: Instant = Instant.now()): Int? {
        return board(LeaderboardKey.of(gameId, period, at)).rankOf(playerId)
    }

    fun scoreOf(gameId: String, playerId: String, period: Period, at: Instant = Instant.now()): Long? {
        return board(LeaderboardKey.of(gameId, period, at)).scoreOf(playerId)
    }

    fun top(gameId: String, period: Period, n: Int, at: Instant = Instant.now()): List<RankedEntry> {
        return board(LeaderboardKey.of(gameId, period, at)).top(n)
    }

    fun around(gameId: String, playerId: String, period: Period, radius: Int, at: Instant = Instant.now()): List<RankedEntry> {
        return board(LeaderboardKey.of(gameId, period, at)).around(playerId, radius)
    }

    fun boardFor(key: LeaderboardKey): ScoreBoard {
        return board(key)
    }

    // a board that is not in memory is rebuilt from the durable copy before it serves anything
    private fun board(key: LeaderboardKey): ScoreBoard {
        lock.withLock {
            boards[key]?.let { return it }

            val board = ScoreBoard(key, newRankedSet(), policy)
            repository.allFor(key).forEach { board.restore(it.playerId, it.score, it.achievedAt) }
            boards[key] = board
            return board
        }
    }
}
