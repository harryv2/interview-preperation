package lld.messaging.notificationengine.strategies.senders

import lld.messaging.notificationengine.entity.Channel
import lld.messaging.notificationengine.entity.Notification

class SmsSender : ChannelSender {
    override val channel = Channel.SMS

    override fun send(notification: Notification) {
        println("  [SMS -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
