package lld.notificationengine.strategies.senders

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification

class SmsSender : ChannelSender {
    override val channel = Channel.SMS

    override fun send(notification: Notification) {
        println("  [SMS -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
