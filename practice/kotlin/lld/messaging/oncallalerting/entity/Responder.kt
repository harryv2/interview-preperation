package lld.messaging.oncallalerting.entity


enum class ChannelType {
    EMAIL,
    SMS,
    IVR
}


enum class Severity {
    SEV1,
    SEV2,
    SEV3
}


class Responder(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
) {

    fun addressFor(channel: ChannelType): String {
        return when (channel) {
            ChannelType.EMAIL -> email
            ChannelType.SMS, ChannelType.IVR -> phone
        }
    }

    override fun toString(): String = name
}
