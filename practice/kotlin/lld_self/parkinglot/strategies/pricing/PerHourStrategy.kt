package lld_self.parkinglot.strategies.pricing

import lld_self.parkinglot.entity.Money
import lld_self.parkinglot.entity.Ticket
import kotlin.time.Instant


class PerHourStrategy : PricingStrategy {

    override fun getPrice(ticket: Ticket, exitTime: Instant): Money {
        var slot = ticket.slot

        var timeDiff = exitTime - ticket.creationTime
        var hours = calculateCeilHours(timeDiff)

        return Money(hours * slotWisePrice[slot.type]!! * 100)
    }

}
