package lld_self.parkinglot.strategies.pricing

import lld_self.parkinglot.entity.Money
import lld_self.parkinglot.entity.Ticket

interface PricingStrategy {
    fun getPrice(ticket: Ticket): Money
}