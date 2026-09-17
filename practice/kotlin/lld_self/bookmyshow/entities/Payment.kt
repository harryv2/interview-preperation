package lld_self.bookmyshow.entities

import kotlin.time.Clock
import kotlin.uuid.Uuid

enum class PaymentMethodTypes {
    UPI,
    CARD,
    WALLET
}

enum class PaymentStatus {
    SUCCESS,
    FAILED
}

data class Payment(
    val amount: Money,
    val method: PaymentMethodTypes,
    val transactionId: String
) {
    val id = Uuid.random()
    val createdAt = Clock.System.now()
}


class PaymentFailedException(message: String): RuntimeException()


interface PaymentMethod {
    fun pay(amount: Money): Payment
}


class UPIPayment: PaymentMethod {
    override fun pay(amount: Money): Payment {
        return Payment(
            amount,
            PaymentMethodTypes.UPI,
            Uuid.random().toString()
        )
    }
}


class CardPayment: PaymentMethod {
    override fun pay(amount: Money): Payment {
        return Payment(
            amount,
            PaymentMethodTypes.CARD,
            Uuid.random().toString()
        )
    }
}


class WalletPayment: PaymentMethod {
    override fun pay(amount: Money): Payment {
        throw PaymentFailedException("Wallet empty")
    }
}