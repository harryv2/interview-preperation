package lld.carrental.strategy

import lld.carrental.entity.CarType
import lld.carrental.entity.Money
import lld.carrental.entity.TimeRange


interface PricingStrategy {
    fun quote(type: CarType, range: TimeRange): Money
    fun lateFeePerDay(type: CarType): Money
}


class PerDayPricing(
    private val dayRate: Map<CarType, Money>
) : PricingStrategy {

    override fun quote(type: CarType, range: TimeRange): Money {
        return rateFor(type) * range.days()
    }

    override fun lateFeePerDay(type: CarType): Money {
        return rateFor(type) + rateFor(type)
    }

    private fun rateFor(type: CarType): Money {
        val rate = dayRate[type]
        requireNotNull(rate) { "No rate for $type" }
        return rate
    }
}
