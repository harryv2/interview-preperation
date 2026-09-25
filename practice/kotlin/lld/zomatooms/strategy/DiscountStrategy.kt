package lld.zomatooms.strategy

import lld.zomatooms.entity.Money


interface DiscountStrategy {
    fun discountOn(itemTotal: Money): Money
}


class NoDiscount : DiscountStrategy {
    override fun discountOn(itemTotal: Money): Money {
        return Money.ZERO
    }
}


class PercentageOffAbove(
    private val percent: Int,
    private val minimumOrder: Money,
    private val cap: Money
) : DiscountStrategy {

    override fun discountOn(itemTotal: Money): Money {
        if (itemTotal < minimumOrder) {
            return Money.ZERO
        }
        return minOf(itemTotal.percent(percent), cap)
    }
}
