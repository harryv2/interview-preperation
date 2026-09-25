package lld.quora.entity

import java.time.Instant
import java.util.UUID


enum class CommentTargetType {
    QUESTION,
    ANSWER
}


class Comment(
    id: String,
    authorId: String,
    body: String,
    createdAt: Instant,
    val targetType: CommentTargetType,
    val targetId: String,
    val parentCommentId: String?,
    val replyingToUserId: String?
) : Content(id, authorId, body, createdAt) {

    override fun votableType(): VotableType {
        return VotableType.COMMENT
    }

    fun isTopLevel(): Boolean {
        return parentCommentId == null
    }

    fun threadRoot(): String {
        return parentCommentId ?: id
    }

    // the comment knows the flattening rule, a reply to a reply lands at level one and records who it is answering
    fun buildReply(authorId: String, body: String): Comment {
        check(!isDeleted) { "Cannot reply to a deleted comment" }
        require(body.isNotBlank()) { "Reply cannot be empty" }

        return Comment(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            body = body,
            createdAt = Instant.now(),
            targetType = this.targetType,
            targetId = this.targetId,
            parentCommentId = this.threadRoot(),
            replyingToUserId = this.authorId
        )
    }

    override fun toString(): String {
        val prefix = if (isTopLevel()) "  " else "      ↳ "
        return "$prefix$body (score ${score()})"
    }
}
