package lld.messaging.notificationengine.strategies.senders

import lld.messaging.notificationengine.entity.Channel
import lld.messaging.notificationengine.entity.Notification

class WhatsAppSender : ChannelSender {
    override val channel = Channel.WHATSAPP

    override fun send(notification: Notification) {
        println("  [WHATSAPP -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
