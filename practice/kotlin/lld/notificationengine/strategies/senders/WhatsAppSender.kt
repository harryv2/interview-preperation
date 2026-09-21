package lld.notificationengine.strategies.senders

import lld.notificationengine.entity.Channel
import lld.notificationengine.entity.Notification

class WhatsAppSender : ChannelSender {
    override val channel = Channel.WHATSAPP

    override fun send(notification: Notification) {
        println("  [WHATSAPP -> ${notification.user.addressFor(channel)}] ${notification.message}")
    }
}
