package lld.commerce.zomatooms

import lld.commerce.zomatooms.entity.Customer
import lld.commerce.zomatooms.entity.DeliveryPartner
import lld.commerce.zomatooms.entity.MenuItem
import lld.commerce.zomatooms.entity.Money
import lld.commerce.zomatooms.entity.Restaurant
import lld.commerce.zomatooms.service.OrderService
import lld.commerce.zomatooms.strategy.FirstFreePartner
import lld.commerce.zomatooms.strategy.PercentageOffAbove

fun main() {

    val biryani = MenuItem("M1", "Chicken Biryani", Money.rupees(280))
    val kebab = MenuItem("M2", "Seekh Kebab", Money.rupees(220))
    val coke = MenuItem("M3", "Coke", Money.rupees(60))

    val paradise = Restaurant("R1", "Paradise", listOf(biryani, kebab, coke))
    val bikanervala = Restaurant("R2", "Bikanervala", listOf(coke), isOpen = false)

    val partners = listOf(DeliveryPartner("D1", "Ravi"), DeliveryPartner("D2", "Asha"))

    val service = OrderService(
        restaurants = listOf(paradise, bikanervala),
        partners = partners,
        discountStrategy = PercentageOffAbove(percent = 10, minimumOrder = Money.rupees(300), cap = Money.rupees(75)),
        assignmentStrategy = FirstFreePartner()
    )

    val asha = Customer("C1", "Asha", "Indiranagar")

    banner("happy path")
    val order = service.placeOrder(asha.id, paradise.id, mapOf(biryani.id to 1, coke.id to 2))
    println("  $order")
    println("  ${order.bill}")
    service.acceptOrder(order.id)
    service.markReady(order.id)
    val partner = service.assignPartner(order.id)
    println("  assigned ${partner.name}")
    service.pickUp(order.id)
    println("  ${service.deliver(order.id)}")
    println("  ${partner.name} free again: ${partner.isFree()}")

    banner("restaurant rejects")
    val rejected = service.placeOrder(asha.id, paradise.id, mapOf(kebab.id to 1))
    service.rejectOrder(rejected.id)
    println("  $rejected")
    attempt("accept a rejected order") {
        service.acceptOrder(rejected.id)
    }

    banner("customer cancels, partner is freed")
    val cancelled = service.placeOrder(asha.id, paradise.id, mapOf(biryani.id to 2))
    service.acceptOrder(cancelled.id)
    service.markReady(cancelled.id)
    val held = service.assignPartner(cancelled.id)
    println("  assigned ${held.name}, free: ${held.isFree()}")
    service.cancelOrder(cancelled.id)
    println("  $cancelled, ${held.name} free again: ${held.isFree()}")

    banner("cancel is too late once it is out for delivery")
    val late = service.placeOrder(asha.id, paradise.id, mapOf(coke.id to 1))
    service.acceptOrder(late.id)
    service.markReady(late.id)
    service.assignPartner(late.id)
    service.pickUp(late.id)
    attempt("cancel while out for delivery") {
        service.cancelOrder(late.id)
    }
    service.deliver(late.id)

    banner("rejected inputs")
    attempt("order from a closed restaurant") {
        service.placeOrder(asha.id, bikanervala.id, mapOf(coke.id to 1))
    }
    attempt("order an item the restaurant does not serve") {
        service.placeOrder(asha.id, paradise.id, mapOf("M9" to 1))
    }
    attempt("empty order") {
        service.placeOrder(asha.id, paradise.id, emptyMap())
    }
    attempt("assign a partner before the order is ready") {
        val fresh = service.placeOrder(asha.id, paradise.id, mapOf(coke.id to 1))
        service.assignPartner(fresh.id)
    }

    banner("order history")
    service.ordersOf(asha.id).forEach { println("  $it") }
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
