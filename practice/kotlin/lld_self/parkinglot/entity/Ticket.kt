package lld_self.parkinglot.entity

import lld_self.parkinglot.strategies.payemnts.Payment
import lld_self.parkinglot.strategies.pricing.PricingStrategy
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class TicketStatus {
    ACTIVE,
    AWAITING_PAYMENT,
    PAID
}

class Ticket(
    val id: Uuid,
    val creationTime: Instant,
    val vehicle: Vehicle,
    val slot: Slot,
    val gateId: String,
) {
    var status: TicketStatus = TicketStatus.ACTIVE
        private set

    var exitTime: Instant? = null
        private set

    var fare: Money? = null
        private set

    var payment: Payment? = null
        private set


    fun markExit(exitTime: Instant, fare: Money) {
        this.exitTime = exitTime
        this.fare = fare
        this.status = TicketStatus.AWAITING_PAYMENT
    }

    fun markPaid(payment: Payment) {
        this.payment = payment
        this.status = TicketStatus.PAID
    }
}
