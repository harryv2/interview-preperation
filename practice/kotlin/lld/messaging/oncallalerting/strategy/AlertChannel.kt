package lld.messaging.oncallalerting.strategy

import lld.messaging.oncallalerting.entity.ChannelType
import lld.messaging.oncallalerting.entity.Incident
import lld.messaging.oncallalerting.entity.Responder


class ChannelFailedException(channel: ChannelType, reason: String) :
    RuntimeException("$channel failed: $reason")


interface AlertChannel {
    val type: ChannelType

    fun page(responder: Responder, incident: Incident)
}


class EmailChannel : AlertChannel {
    override val type = ChannelType.EMAIL

    override fun page(responder: Responder, incident: Incident) {
        println("    [EMAIL -> ${responder.addressFor(type)}] ${incident.severity} ${incident.title}")
    }
}


class SmsChannel : AlertChannel {
    override val type = ChannelType.SMS

    override fun page(responder: Responder, incident: Incident) {
        println("    [SMS   -> ${responder.addressFor(type)}] ${incident.severity} ${incident.title}")
    }
}


class IvrChannel : AlertChannel {
    override val type = ChannelType.IVR

    override fun page(responder: Responder, incident: Incident) {
        println("    [IVR   -> ${responder.addressFor(type)}] calling, press 1 to acknowledge ${incident.id}")
    }
}


// stands in for a carrier having a bad day, so the demo can show one channel dying without taking the page
// down with it
class FlakyChannel(private val delegate: AlertChannel, private val failEvery: Int) : AlertChannel {

    override val type = delegate.type
    private var calls = 0

    override fun page(responder: Responder, incident: Incident) {
        calls += 1
        if (calls % failEvery == 0) throw ChannelFailedException(type, "carrier timeout")

        delegate.page(responder, incident)
    }
}
