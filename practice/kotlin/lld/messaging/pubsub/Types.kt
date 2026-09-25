package lld.messaging.pubsub

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val payload: String,
)

typealias Handler = (Message) -> Unit

data class DeadLetter(
    val message: Message,
    val error: String,
)

data class Config(
    val maxAttempts: Int = 3,
    val retryDelayMs: Long = 100,
)
