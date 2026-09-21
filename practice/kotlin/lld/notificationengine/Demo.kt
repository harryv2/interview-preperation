package lld.notificationengine

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification
import lld.notificationengine.entity.NotificationService
import lld.notificationengine.entity.RetryPolicy
import lld.notificationengine.entity.User
import lld.notificationengine.strategies.ratelimit.CompositeRateLimiter
import lld.notificationengine.strategies.ratelimit.PerUserRateLimiter
import lld.notificationengine.strategies.ratelimit.RateLimiter
import lld.notificationengine.strategies.ratelimit.SlidingCounterRateLimiter
import lld.notificationengine.strategies.ratelimit.SlidingWindowRateLimiter
import lld.notificationengine.strategies.ratelimit.TokenBucketRateLimiter
import lld.notificationengine.strategies.senders.ChannelSender
import lld.notificationengine.strategies.senders.EmailSender
import lld.notificationengine.strategies.senders.PushSender
import lld.notificationengine.strategies.senders.SmsSender
import lld.notificationengine.strategies.senders.WhatsAppSender
import kotlin.time.Duration.Companion.days

class FlakySender(private val delegate: ChannelSender, private var failures: Int) : ChannelSender {
    override val channel = delegate.channel

    override fun send(notification: Notification) {
        if (failures > 0) {
            failures--
            throw RuntimeException("$channel provider timeout")
        }
        delegate.send(notification)
    }
}

fun main() {
    val service = NotificationService(
        senders = listOf(
            SmsSender(),
            EmailSender(),
            FlakySender(PushSender(), failures = 2),
            FlakySender(WhatsAppSender(), failures = 5),
        ),
        rateLimiters = mapOf(
            Channel.SMS to limits(
                burst = TokenBucketRateLimiter(capacity = 2, refillPerSecond = 5.0),
                perUserPerDay = { SlidingWindowRateLimiter(2, 1.days) },
                channelPerDay = SlidingWindowRateLimiter(5, 1.days),
            ),
            Channel.WHATSAPP to limits(
                burst = TokenBucketRateLimiter(capacity = 10, refillPerSecond = 1.0),
                perUserPerDay = { SlidingWindowRateLimiter(5, 1.days) },
                channelPerDay = SlidingWindowRateLimiter(500, 1.days),
            ),
            Channel.EMAIL to limits(
                burst = TokenBucketRateLimiter(capacity = 100, refillPerSecond = 50.0),
                perUserPerDay = { SlidingCounterRateLimiter(20, 1.days) },
                channelPerDay = SlidingCounterRateLimiter(10_000, 1.days),
            ),
            Channel.PUSH to limits(
                burst = TokenBucketRateLimiter(capacity = 50, refillPerSecond = 50.0),
                perUserPerDay = { SlidingCounterRateLimiter(50, 1.days) },
                channelPerDay = SlidingCounterRateLimiter(5_000, 1.days),
            ),
        ),
        retryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 150),
    )
    val user = User("u1", phone = "+91-9999999999", email = "aaryan@example.com", pushToken = "tok-123")
    val other = User("u2", phone = "+91-8888888888")

    println("-- 3 SMS to u1: burst bucket holds 2 (refills 5/sec), u1 is capped at 2/day")
    for (i in 0 until 3) {
        report(service.send(user, Channel.SMS, "OTP ${1000 + i}").join())
    }

    println("-- u2 still has quota, channel has room (5/day)")
    report(service.send(other, Channel.SMS, "OTP 2000").join())

    println("-- push provider fails twice, third attempt succeeds")
    report(service.send(user, Channel.PUSH, "Your order shipped").join())

    println("-- whatsapp provider keeps failing, gives up after max attempts")
    report(service.send(user, Channel.WHATSAPP, "Delivery today").join())

    println("-- email, plain success; send() returns before delivery")
    val pending = service.send(user, Channel.EMAIL, "Invoice attached")
    println("  returned to caller, done yet: ${pending.isDone}")
    report(pending.join())

    service.shutdown()
}

private fun limits(burst: RateLimiter, perUserPerDay: () -> RateLimiter, channelPerDay: RateLimiter): RateLimiter {
    return CompositeRateLimiter(
        listOf(
            burst,
            PerUserRateLimiter(perUserPerDay),
            channelPerDay,
        ),
    )
}

private fun report(n: Notification) {
    val error = if (n.lastError != null) {
        ", last error: ${n.lastError}"
    } else {
        ""
    }
    println("  ${n.channel} ${n.status} after ${n.attempts} attempt(s)$error")
}
