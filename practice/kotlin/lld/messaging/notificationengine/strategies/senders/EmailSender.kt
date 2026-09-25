package lld.messaging.notificationengine.strategies.senders

import lld.messaging.notificationengine.entity.Channel
import lld.messaging.notificationengine.entity.Notification

class EmailSender : ChannelSender {
    override val channel = Channel.EMAIL

    override fun send(notification: Notification) {
        println("  [EMAIL -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
