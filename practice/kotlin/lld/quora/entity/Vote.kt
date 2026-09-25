package lld.quora.entity

import java.time.Instant


enum class VoteValue(val delta: Int) {
    UP(1),
    DOWN(-1)
}

enum class VoteOutcome {
    ADDED,
    REMOVED,
    SWITCHED
}


class Vote(
    val id: String,
    val voterId: String,
    val targetType: VotableType,
    val targetId: String,
    val value: VoteValue,
    val castAt: Instant
)
