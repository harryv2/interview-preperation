package lld.quora.entity

import java.time.Instant


class Question(
    id: String,
    authorId: String,
    val title: String,
    body: String,
    createdAt: Instant
) : Content(id, authorId, body, createdAt) {

    private val topicIds = mutableSetOf<String>()
    private val answerIds = mutableListOf<String>()

    var isClosed: Boolean = false
        private set

    override fun votableType(): VotableType {
        return VotableType.QUESTION
    }

    // the question enforces its own rules, nothing outside can reach into answerIds and skip the check
    fun addAnswer(answerId: String) {
        check(!isClosed) { "Cannot answer a closed question" }
        check(!isDeleted) { "Cannot answer a deleted question" }
        answerIds.add(answerId)
    }

    fun tagWith(topicId: String) {
        check(topicIds.size < MAX_TOPICS) { "A question can carry at most $MAX_TOPICS topics" }
        topicIds.add(topicId)
    }

    fun untag(topicId: String) {
        topicIds.remove(topicId)
    }

    fun close(requesterId: String) {
        require(requesterId == authorId) { "Only the author can close" }
        isClosed = true
    }

    fun answerIds(): List<String> {
        return answerIds.toList()
    }

    fun topicIds(): Set<String> {
        return topicIds.toSet()
    }

    fun answerCount(): Int {
        return answerIds.size
    }

    override fun toString(): String {
        return "Q(${id.take(6)}) \"$title\" score=${score()} answers=${answerIds.size}"
    }

    companion object {
        const val MAX_TOPICS = 5
    }
}
