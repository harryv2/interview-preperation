package lld_self.parkinglot.entity

import lld_self.parkinglot.strategies.payemnts.Payment
import lld_self.parkinglot.strategies.pricing.PricingStrategy
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Ticket(
    val id: Uuid,
    val creationTime: Instant,
    val vehicle: Vehicle,
    val slot: Slot,
    val gateId: String,
) {
    var payment: Payment? = null
        private set


    fun markPaid(payment: Payment) {
        this.payment = payment
    }
}