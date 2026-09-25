package lld.quora.entity

import java.time.Instant


class User(
    val id: String,
    val name: String,
    val email: String,
    val joinedAt: Instant
)


// stored, not derived: counting every vote on everything a person ever wrote, on every profile load, does not scale
class UserStats {

    var questionCount: Int = 0
        private set

    var answerCount: Int = 0
        private set

    var commentCount: Int = 0
        private set

    var upvotesReceived: Int = 0
        private set

    var downvotesReceived: Int = 0
        private set

    fun recordQuestion() {
        questionCount++
    }

    fun recordAnswer() {
        answerCount++
    }

    fun recordComment() {
        commentCount++
    }

    fun recordVoteReceived(value: VoteValue, direction: Int) {
        if (value == VoteValue.UP) {
            upvotesReceived += direction
        } else {
            downvotesReceived += direction
        }
    }

    // a method, not a field: weights change, and you do not want to recompute a stored number across every user when they do
    fun reputation(): Int {
        return upvotesReceived * 10 - downvotesReceived * 2
    }
}
