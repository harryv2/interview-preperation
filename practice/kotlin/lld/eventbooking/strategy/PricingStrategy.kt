package lld.eventbooking.strategy

import lld.eventbooking.entity.Money
import lld.eventbooking.entity.TicketTier


interface PricingStrategy {
    fun priceFor(tier: TicketTier, quantity: Int, soldFraction: Double): Money
}


class FlatTierPricing : PricingStrategy {
    override fun priceFor(tier: TicketTier, quantity: Int, soldFraction: Double): Money {
        return tier.basePrice * quantity
    }
}


// a concert moves its price as the house fills, each quarter sold adds one step
class DemandPricing(private val stepPercent: Int) : PricingStrategy {

    override fun priceFor(tier: TicketTier, quantity: Int, soldFraction: Double): Money {
        val steps = (soldFraction * 4).toInt().coerceIn(0, 4)
        return (tier.basePrice * quantity).percent(100 + steps * stepPercent)
    }
}
