package lld.eventbooking.entity

import java.time.Instant


enum class PaymentMethodKind {
    CARD,
    UPI,
    NETBANKING
}


class Payment(
    val id: String,
    val amount: Money,
    val kind: PaymentMethodKind,
    val reference: String,
    val paidAt: Instant
) {
    override fun toString(): String {
        return "$kind $amount ref $reference"
    }
}
