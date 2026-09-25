package lld.leaderboard.strategy


interface ScorePolicy {
    fun merge(previous: Long?, submitted: Long): Long
}


// arcade style, only your personal best stands and a worse run changes nothing
class BestScoreWins : ScorePolicy {
    override fun merge(previous: Long?, submitted: Long): Long {
        if (previous == null) {
            return submitted
        }
        return maxOf(previous, submitted)
    }
}


// the most recent run is the one on the board
class LatestScoreWins : ScorePolicy {
    override fun merge(previous: Long?, submitted: Long): Long {
        return submitted
    }
}


// xp, season points, coins collected
class CumulativeScore : ScorePolicy {
    override fun merge(previous: Long?, submitted: Long): Long {
        return (previous ?: 0) + submitted
    }
}
