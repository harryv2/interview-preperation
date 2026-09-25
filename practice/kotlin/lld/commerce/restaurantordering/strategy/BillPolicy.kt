package lld.commerce.restaurantordering.strategy

import lld.commerce.restaurantordering.entity.Bill
import lld.commerce.restaurantordering.entity.Money
import lld.commerce.restaurantordering.entity.Order


interface BillPolicy {
    fun bill(order: Order): Bill
}

class DineInBillPolicy(
    private val serviceChargePercent: Int,
    private val gstPercent: Int,
    private val discountPolicy: DiscountPolicy
) : BillPolicy {

    override fun bill(order: Order): Bill {
        val subtotal = order.subtotal()
        val discount = discountPolicy.on(subtotal)
        val taxable = subtotal - discount
        val serviceCharge = taxable.percent(serviceChargePercent)

        return Bill(
            orderId = order.id,
            subtotal = subtotal,
            discount = discount,
            serviceCharge = serviceCharge,
            tax = (taxable + serviceCharge).percent(gstPercent)
        )
    }
}

class TakeawayBillPolicy(
    private val gstPercent: Int,
    private val discountPolicy: DiscountPolicy
) : BillPolicy {

    override fun bill(order: Order): Bill {
        val subtotal = order.subtotal()
        val discount = discountPolicy.on(subtotal)
        val taxable = subtotal - discount

        return Bill(
            orderId = order.id,
            subtotal = subtotal,
            discount = discount,
            serviceCharge = Money.ZERO,
            tax = taxable.percent(gstPercent)
        )
    }
}
