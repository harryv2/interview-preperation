package lld.notificationengine.strategies.senders

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification

class PushSender : ChannelSender {
    override val channel = Channel.PUSH

    override fun send(notification: Notification) {
        println("  [PUSH -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
