package lld.commerce.restaurantordering

import lld.commerce.restaurantordering.entity.Course
import lld.commerce.restaurantordering.entity.DiningTable
import lld.commerce.restaurantordering.entity.Menu
import lld.commerce.restaurantordering.entity.MenuItem
import lld.commerce.restaurantordering.entity.Money
import lld.commerce.restaurantordering.entity.Station
import lld.commerce.restaurantordering.service.KitchenDisplay
import lld.commerce.restaurantordering.service.LineRequest
import lld.commerce.restaurantordering.service.RestaurantService
import lld.commerce.restaurantordering.service.WaiterDevice
import lld.commerce.restaurantordering.strategy.DineInBillPolicy
import lld.commerce.restaurantordering.strategy.SpendOverDiscount

fun main() {

    val menu = Menu(
        listOf(
            MenuItem("m1", "Paneer Tikka", Course.STARTER, Station.KITCHEN, Money.rupees(320)),
            MenuItem("m2", "Dal Makhani", Course.MAIN, Station.KITCHEN, Money.rupees(280)),
            MenuItem("m3", "Butter Naan", Course.MAIN, Station.KITCHEN, Money.rupees(70)),
            MenuItem("m4", "Gulab Jamun", Course.DESSERT, Station.KITCHEN, Money.rupees(120)),
            MenuItem("m5", "Masala Soda", Course.DRINK, Station.BAR, Money.rupees(90)),
            MenuItem("m6", "Cold Coffee", Course.DRINK, Station.BAR, Money.rupees(150))
        )
    )

    val tables = listOf(
        DiningTable("T1", 2),
        DiningTable("T2", 4),
        DiningTable("T3", 6)
    )

    val service = RestaurantService(
        tables = tables,
        menu = menu,
        billPolicy = DineInBillPolicy(
            serviceChargePercent = 5,
            gstPercent = 5,
            discountPolicy = SpendOverDiscount(Money.rupees(1000), 10)
        )
    )

    val device = WaiterDevice("PAD-1", "W-Ravi", service)
    val kitchen = KitchenDisplay(Station.KITCHEN, service)
    val bar = KitchenDisplay(Station.BAR, service)

    println("free tables ${device.freeTables()}")

    val order = device.openTable("T2")
    println("opened $order")

    println("\n-- first punch, starters and drinks --")
    device.punch(
        order.id,
        LineRequest("m1", 2, "less spicy"),
        LineRequest("m5", 2)
    )

    println("kitchen queue ${kitchen.queue()}")
    println("bar queue     ${bar.queue()}")

    val starterKot = kitchen.queue().first()
    val drinksKot = bar.queue().first()

    bar.accept(drinksKot.id)
    bar.ready(drinksKot.id)
    drinksKot.lines.forEach { device.serve(order.id, it.id) }

    kitchen.accept(starterKot.id)
    kitchen.ready(starterKot.id)
    starterKot.lines.forEach { device.serve(order.id, it.id) }

    println("\n-- second punch, mains, one of them gets cancelled --")
    val mainsKot = device.punch(
        order.id,
        LineRequest("m2", 1),
        LineRequest("m3", 4),
        LineRequest("m6", 1)
    ).first { it.station == Station.KITCHEN }

    val naan = mainsKot.lines.first { it.item.id == "m3" }
    device.cancel(order.id, naan.id)
    println("after cancel ${order.lines()}")

    // the kitchen raises the price mid meal, the punched lines keep the price they were punched at
    menu.reprice("m2", Money.rupees(400))

    kitchen.accept(mainsKot.id)
    kitchen.ready(mainsKot.id)

    val coffeeKot = bar.queue().first()
    bar.accept(coffeeKot.id)
    bar.ready(coffeeKot.id)

    order.pendingLines().forEach { device.serve(order.id, it.id) }

    println("\n-- bill --")
    val bill = device.printBill(order.id)
    println(bill)

    device.settle(order.id, bill.total)
    println("\nsettled $order")
    println("free tables ${device.freeTables()}")
}
