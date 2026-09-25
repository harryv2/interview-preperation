package lld.messaging.notificationengine.entity

data class User(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    val pushToken: String? = null,
) {
    fun addressFor(channel: Channel): String {
        val address = when (channel) {
            Channel.SMS, Channel.WHATSAPP -> phone
            Channel.EMAIL -> email
            Channel.PUSH -> pushToken
        }
        return requireNotNull(address) { "User $id has no address for $channel" }
    }
}
