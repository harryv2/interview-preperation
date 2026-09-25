package lld.games.leaderboard.entity

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields


enum class Period {
    DAILY,
    WEEKLY,
    ALL_TIME;

    // the bucket is what makes a daily board a different board every day, and lets yesterday's be dropped whole
    fun bucketFor(at: Instant): String {
        val date = at.atZone(ZoneOffset.UTC).toLocalDate()
        return when (this) {
            DAILY -> date.toString()
            WEEKLY -> "${date.get(IsoFields.WEEK_BASED_YEAR)}-W${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            ALL_TIME -> "all"
        }
    }
}


data class LeaderboardKey(
    val gameId: String,
    val period: Period,
    val bucket: String
) {
    override fun toString(): String {
        return "$gameId/$period/$bucket"
    }

    companion object {
        fun of(gameId: String, period: Period, at: Instant): LeaderboardKey {
            return LeaderboardKey(gameId, period, period.bucketFor(at))
        }
    }
}
