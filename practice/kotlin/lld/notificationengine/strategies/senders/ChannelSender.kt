package lld.notificationengine.strategies.senders

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification

interface ChannelSender {
    val channel: Channel

    fun send(notification: Notification)
}

class RateLimitedException(channel: Channel) : RuntimeException("$channel rate limit hit")
