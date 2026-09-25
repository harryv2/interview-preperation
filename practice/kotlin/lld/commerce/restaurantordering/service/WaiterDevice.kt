package lld.commerce.restaurantordering.service

import lld.commerce.restaurantordering.entity.Bill
import lld.commerce.restaurantordering.entity.DiningTable
import lld.commerce.restaurantordering.entity.Kot
import lld.commerce.restaurantordering.entity.MenuItem
import lld.commerce.restaurantordering.entity.Money
import lld.commerce.restaurantordering.entity.Order


class WaiterDevice(
    val deviceId: String,
    val waiterId: String,
    private val service: RestaurantService
) {

    private var punchCounter = 0

    fun freeTables(): List<DiningTable> {
        return service.freeTables()
    }

    fun menu(): List<MenuItem> {
        return service.menu.available()
    }

    fun openTable(tableNumber: String): Order {
        return service.openTable(tableNumber, waiterId)
    }

    // the punch id is minted on the device, so a resend after a dropped reply lands as the same ticket
    fun punch(orderId: String, vararg requests: LineRequest): List<Kot> {
        punchCounter += 1
        return service.punch(orderId, "$deviceId-$punchCounter", requests.toList())
    }

    fun resendLastPunch(orderId: String, vararg requests: LineRequest): List<Kot> {
        return service.punch(orderId, "$deviceId-$punchCounter", requests.toList())
    }

    fun cancel(orderId: String, lineId: String) {
        service.cancelLine(orderId, lineId)
    }

    fun serve(orderId: String, lineId: String) {
        service.markServed(orderId, lineId)
    }

    fun printBill(orderId: String): Bill {
        return service.generateBill(orderId)
    }

    fun settle(orderId: String, paid: Money): Order {
        return service.settle(orderId, paid)
    }
}
