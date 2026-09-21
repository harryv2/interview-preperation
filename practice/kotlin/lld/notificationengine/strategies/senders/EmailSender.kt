package lld.notificationengine.strategies.senders

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification

class EmailSender : ChannelSender {
    override val channel = Channel.EMAIL

    override fun send(notification: Notification) {
        println("  [EMAIL -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
