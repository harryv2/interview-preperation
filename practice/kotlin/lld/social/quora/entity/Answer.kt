package lld.social.quora.entity

import java.time.Instant


class Answer(
    id: String,
    authorId: String,
    body: String,
    createdAt: Instant,
    val questionId: String
) : Content(id, authorId, body, createdAt) {

    override fun votableType(): VotableType {
        return VotableType.ANSWER
    }

    override fun toString(): String {
        val preview = if (body.length > 40) body.take(40) + "..." else body
        return "A(${id.take(6)}) \"$preview\" ${upvotes}↑ ${downvotes}↓ score=${score()}"
    }
}
