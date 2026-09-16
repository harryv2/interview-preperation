package lld_self.parkinglot.strategies.pricing

import lld_self.parkinglot.entity.Money
import lld_self.parkinglot.entity.SlotType
import lld_self.parkinglot.entity.Ticket
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit


var slotWisePrice =  hashMapOf<SlotType, Long>(
    SlotType.CAR to 10,
    SlotType.TRUCK to 20,
    SlotType.BIKE to 5
)

fun calculateCeilHours(duration: Duration): Long {
    return ceil(duration.toDouble(DurationUnit.HOURS)).toLong()
}

class FlatPricingStrategy : PricingStrategy {

    override fun getPrice(ticket: Ticket): Money {
        var slot = ticket.slot
        var now = Clock.System.now()

        var timeDiff = now - ticket.creationTime
        var hours = calculateCeilHours(timeDiff)

        return Money(hours * slotWisePrice[slot.type]!! * 100)
    }

}