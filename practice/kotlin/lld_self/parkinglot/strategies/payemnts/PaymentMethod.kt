package lld_self.parkinglot.strategies.payemnts

import lld_self.parkinglot.entity.Money
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class PaymentMethod {
    CARD,
    UPI,
    FASTAG
}

data class Payment(
    val id: Uuid,
    val creationTime: Instant,
    val transactionId: String,
    val method: PaymentMethod
)


interface PaymentMethodStrategy {
    fun pay(money: Money): Payment
}