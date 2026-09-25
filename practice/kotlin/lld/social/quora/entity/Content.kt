package lld.social.quora.entity

import java.time.Instant


enum class VotableType {
    QUESTION,
    ANSWER,
    COMMENT
}


// a base class rather than an interface because the vote counting implementation is shared, not just the contract
abstract class Content(
    val id: String,
    val authorId: String,
    body: String,
    val createdAt: Instant
) {

    var body: String = body
        private set

    var upvotes: Int = 0
        private set

    var downvotes: Int = 0
        private set

    var isDeleted: Boolean = false
        private set

    abstract fun votableType(): VotableType

    fun score(): Int {
        return upvotes - downvotes
    }

    // only the vote service calls this, always under its lock
    internal fun applyVote(value: VoteValue, direction: Int) {
        if (value == VoteValue.UP) {
            upvotes += direction
        } else {
            downvotes += direction
        }
    }

    fun edit(newBody: String, editorId: String) {
        require(editorId == authorId) { "Only the author can edit" }
        require(!isDeleted) { "Cannot edit deleted content" }
        require(newBody.isNotBlank()) { "Body cannot be empty" }
        body = newBody
    }

    fun softDelete(requesterId: String) {
        require(requesterId == authorId) { "Only the author can delete" }
        isDeleted = true
    }
}
