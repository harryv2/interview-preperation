package lld.commerce.restaurantordering.strategy

import lld.commerce.restaurantordering.entity.Money


interface DiscountPolicy {
    fun on(subtotal: Money): Money
}

class NoDiscount : DiscountPolicy {
    override fun on(subtotal: Money): Money {
        return Money.ZERO
    }
}

class PercentDiscount(private val percent: Int) : DiscountPolicy {
    override fun on(subtotal: Money): Money {
        return subtotal.percent(percent)
    }
}

class SpendOverDiscount(
    private val threshold: Money,
    private val percent: Int
) : DiscountPolicy {
    override fun on(subtotal: Money): Money {
        if (subtotal < threshold) {
            return Money.ZERO
        }
        return subtotal.percent(percent)
    }
}
