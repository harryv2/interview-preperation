package lld.messaging.notificationengine.strategies.senders

import lld.messaging.notificationengine.entity.Channel
import lld.messaging.notificationengine.entity.Notification

class PushSender : ChannelSender {
    override val channel = Channel.PUSH

    override fun send(notification: Notification) {
        println("  [PUSH -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
