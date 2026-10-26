package lld.rippling.articlevotes

enum class Vote { LIKE, DISLIKE }

data class VoteSwitch(
    val articleId: String,
    val from: Vote,
    val to: Vote,
)

class ArticleCounts {
    var likes = 0
    var dislikes = 0
}

class ArticleVotes(private val historyLimit: Int = 3) {
    private val votes = HashMap<Pair<String, String>, Vote>()
    private val counts = HashMap<String, ArticleCounts>()
    private val switches = HashMap<String, ArrayDeque<VoteSwitch>>()

    fun like(userId: String, articleId: String) {
        cast(userId, articleId, Vote.LIKE)
    }

    fun dislike(userId: String, articleId: String) {
        cast(userId, articleId, Vote.DISLIKE)
    }

    fun clear(userId: String, articleId: String) {
        val previous = votes.remove(userId to articleId) ?: return
        adjust(articleId, previous, -1)
    }

    fun likes(articleId: String): Int {
        return counts[articleId]?.likes ?: 0
    }

    fun dislikes(articleId: String): Int {
        return counts[articleId]?.dislikes ?: 0
    }

    fun recentSwitches(userId: String): List<VoteSwitch> {
        return switches[userId]?.reversed() ?: emptyList()
    }

    private fun cast(userId: String, articleId: String, vote: Vote) {
        val previous = votes[userId to articleId]
        if (previous == vote) {
            return
        }
        votes[userId to articleId] = vote
        adjust(articleId, vote, 1)

        if (previous != null) {
            adjust(articleId, previous, -1)
            recordSwitch(userId, VoteSwitch(articleId, previous, vote))
        }
    }

    private fun adjust(articleId: String, vote: Vote, delta: Int) {
        val articleCounts = counts.getOrPut(articleId) { ArticleCounts() }
        when (vote) {
            Vote.LIKE -> articleCounts.likes += delta
            Vote.DISLIKE -> articleCounts.dislikes += delta
        }
    }

    private fun recordSwitch(userId: String, switch: VoteSwitch) {
        val history = switches.getOrPut(userId) { ArrayDeque() }
        history.addLast(switch)
        if (history.size > historyLimit) {
            history.removeFirst()
        }
    }
}
