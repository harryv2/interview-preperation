package lld.messaging.notificationengine.entity

import kotlin.uuid.Uuid

enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
}

class Notification(
    val id: Uuid,
    val user: User,
    val channel: Channel,
    val message: String,
) {
    @Volatile var status = NotificationStatus.PENDING
        private set

    @Volatile var attempts = 0
        private set

    @Volatile var lastError: String? = null
        private set

    fun recordAttempt(error: String?) {
        attempts++
        lastError = error
    }

    fun markSent() {
        status = NotificationStatus.SENT
    }

    fun markFailed() {
        status = NotificationStatus.FAILED
    }
}
