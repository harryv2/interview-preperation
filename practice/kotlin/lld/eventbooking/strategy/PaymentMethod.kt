package lld.eventbooking.strategy

import lld.eventbooking.entity.Money
import lld.eventbooking.entity.Payment
import lld.eventbooking.entity.PaymentMethodKind
import java.time.Instant
import java.util.UUID


sealed interface PaymentResult {
    data class Success(val payment: Payment) : PaymentResult
    data class Declined(val reason: String) : PaymentResult
    data class Unavailable(val reason: String) : PaymentResult
}


// the key is the order id, a retry after a dropped response must not charge the card twice
interface PaymentMethod {
    fun pay(amount: Money, idempotencyKey: String): PaymentResult
}


class CardMethod : PaymentMethod {
    override fun pay(amount: Money, idempotencyKey: String): PaymentResult {
        return PaymentResult.Success(
            Payment(UUID.randomUUID().toString().take(8), amount, PaymentMethodKind.CARD, idempotencyKey, Instant.now())
        )
    }
}


class UpiMethod : PaymentMethod {
    override fun pay(amount: Money, idempotencyKey: String): PaymentResult {
        return PaymentResult.Success(
            Payment(UUID.randomUUID().toString().take(8), amount, PaymentMethodKind.UPI, idempotencyKey, Instant.now())
        )
    }
}


class DecliningCard(private val reason: String = "Insufficient funds") : PaymentMethod {
    override fun pay(amount: Money, idempotencyKey: String): PaymentResult {
        return PaymentResult.Declined(reason)
    }
}
