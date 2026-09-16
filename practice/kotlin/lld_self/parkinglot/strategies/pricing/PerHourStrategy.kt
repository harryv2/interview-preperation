package lld_self.parkinglot.strategies.pricing

import lld_self.parkinglot.entity.Money
import lld_self.parkinglot.entity.Ticket
import kotlin.time.Clock


class PerHourStrategy : PricingStrategy {

    override fun getPrice(ticket: Ticket): Money {
        var slot = ticket.slot
        var now = Clock.System.now()

        var timeDiff = now - ticket.creationTime
        var hours = calculateCeilHours(timeDiff)

        return Money(hours * slotWisePrice[slot.type]!! * 100)
    }

}