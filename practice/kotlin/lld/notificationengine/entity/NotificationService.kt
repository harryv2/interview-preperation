package lld.notificationengine.entity

import lld.notificationengine.strategies.ratelimit.RateLimiter
import lld.notificationengine.strategies.senders.ChannelSender
import lld.notificationengine.strategies.senders.RateLimitedException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

class NotificationService(
    senders: List<ChannelSender>,
    private val rateLimiters: Map<Channel, RateLimiter>,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(4),
) {
    private val senders = senders.associateBy { it.channel }

    fun send(user: User, channel: Channel, message: String): CompletableFuture<Notification> {
        requireNotNull(senders[channel]) { "No sender for $channel" }
        requireNotNull(rateLimiters[channel]) { "No rate limit for $channel" }

        val notification = Notification(Uuid.random(), user, channel, message)
        val outcome = CompletableFuture<Notification>()

        scheduler.execute {
            attempt(notification, 1, outcome)
        }
        return outcome
    }

    fun shutdown() {
        scheduler.shutdown()
        scheduler.awaitTermination(10, TimeUnit.SECONDS)
    }

    private fun attempt(notification: Notification, attempt: Int, outcome: CompletableFuture<Notification>) {
        val sender = senders.getValue(notification.channel)
        val limiter = rateLimiters.getValue(notification.channel)

        try {
            if (!limiter.tryAcquire(notification.user.id)) {
                throw RateLimitedException(notification.channel)
            }
            sender.send(notification)
            notification.recordAttempt(null)
            notification.markSent()
            outcome.complete(notification)
        } catch (e: Exception) {
            notification.recordAttempt(e.message ?: e.toString())

            if (attempt >= retryPolicy.maxAttempts) {
                notification.markFailed()
                outcome.complete(notification)
            } else {
                val next = attempt + 1
                val delay = retryPolicy.delayBeforeAttempt(next)
                scheduler.schedule({ attempt(notification, next, outcome) }, delay, TimeUnit.MILLISECONDS)
            }
        }
    }
}
