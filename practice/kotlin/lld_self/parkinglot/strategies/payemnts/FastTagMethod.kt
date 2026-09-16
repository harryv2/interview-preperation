package lld_self.parkinglot.strategies.payemnts

import lld_self.parkinglot.entity.Money
import kotlin.time.Clock
import kotlin.uuid.Uuid

class FastTagMethod : PaymentMethodStrategy {
    override fun pay(money: Money): Payment {
        return Payment(
            Uuid.random(),
            Clock.System.now(),
            Uuid.random().toString(),
            PaymentMethod.FASTAG
        )
    }
}