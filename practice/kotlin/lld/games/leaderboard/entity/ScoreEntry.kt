package lld.games.leaderboard.entity

import java.time.Instant


class ScoreEntry(
    val playerId: String,
    val score: Long,
    val achievedAt: Instant
) {
    override fun toString(): String {
        return "$playerId $score"
    }

    companion object {
        // rank 1 is the best, so the set is ordered by score descending, then by who got there first
        val ORDER: Comparator<ScoreEntry> = compareByDescending<ScoreEntry> { it.score }
            .thenBy { it.achievedAt }
            .thenBy { it.playerId }
    }
}


class RankedEntry(
    val rank: Int,
    val entry: ScoreEntry
) {
    override fun toString(): String {
        return "#$rank ${entry.playerId} ${entry.score}"
    }
}
