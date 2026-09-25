package lld.booking.restaurantreservation

import lld.booking.restaurantreservation.entity.Customer
import lld.booking.restaurantreservation.entity.Table
import lld.booking.restaurantreservation.service.Restaurant
import lld.booking.restaurantreservation.strategy.SmallestFitStrategy
import java.time.LocalDate
import java.time.LocalTime

fun main() {
    val tables = listOf(
        Table("T1", 2),
        Table("T2", 4),
        Table("T3", 4),
        Table("T4", 8)
    )
    val restaurant = Restaurant("Bistro", tables, SmallestFitStrategy())

    val asha = Customer("C1", "Asha", "9000000001")
    val ravi = Customer("C2", "Ravi", "9000000002")
    val kiran = Customer("C3", "Kiran", "9000000003")
    val nina = Customer("C4", "Nina", "9000000004")
    val meera = Customer("C5", "Meera", "9000000005")
    val dev = Customer("C6", "Dev", "9000000006")

    val tomorrow = LocalDate.now().plusDays(1)
    val seven = tomorrow.atTime(LocalTime.of(19, 0))
    val eight = tomorrow.atTime(LocalTime.of(20, 0))
    val nine = tomorrow.atTime(LocalTime.of(21, 0))

    println("free for 3 at 19:00 -> ${restaurant.availableTables(3, seven).map { it.id }}")

    val r1 = restaurant.reserve(asha, 3, seven)
    println("asha 3 @19:00 -> ${r1?.table?.id}")

    val r2 = restaurant.reserve(ravi, 3, seven)
    println("ravi 3 @19:00 -> ${r2?.table?.id}")

    val r3 = restaurant.reserve(kiran, 5, seven)
    println("kiran 5 @19:00 -> ${r3?.table?.id}")

    val r4 = restaurant.reserve(nina, 2, seven)
    println("nina 2 @19:00 -> ${r4?.table?.id}")

    val r5 = restaurant.reserve(ravi, 3, eight)
    println("ravi 3 @20:00 -> ${r5?.table?.id}")

    val r6 = restaurant.reserve(asha, 3, nine)
    println("asha 3 @21:00 -> ${r6?.table?.id}")

    val r7 = restaurant.reserve(meera, 3, seven)
    println("meera 3 @19:00 -> ${r7?.table?.id}")

    val w1 = restaurant.joinWaitlist(meera, 3, seven)
    val w2 = restaurant.joinWaitlist(dev, 4, seven)
    val w3 = restaurant.joinWaitlist(asha, 2, seven)
    println("waiting for 19:00 -> ${restaurant.waitingFor(seven).map { "${it.customer.name}:${it.partySize}" }}")

    restaurant.leaveWaitlist(w3.id)
    println("asha left -> ${w3.status}, waiting -> ${restaurant.waitingFor(seven).map { it.customer.name }}")

    restaurant.cancel(r1!!.id)
    println("asha cancelled @19:00 -> meera ${w1.status} on ${w1.reservation?.table?.id}, dev ${w2.status}")

    restaurant.checkIn(r2!!.id)
    restaurant.complete(r2.id)
    println("ravi @19:00 ${r2.status} -> dev ${w2.status} on ${w2.reservation?.table?.id}")

    println("waiting for 19:00 -> ${restaurant.waitingFor(seven).map { it.customer.name }}")
    println("free for 3 at 19:00 -> ${restaurant.availableTables(3, seven).map { it.id }}")

    println("meera's reservations -> ${restaurant.reservationsOf(meera).map { "${it.table.id}:${it.status}" }}")
    println("asha's waitlist -> ${restaurant.waitlistOf(asha).map { it.status }}")

    try {
        restaurant.joinWaitlist(asha, 10, seven)
    } catch (e: IllegalArgumentException) {
        println("rejected: ${e.message}")
    }

    try {
        restaurant.reserve(asha, 2, tomorrow.atTime(LocalTime.of(22, 30)))
    } catch (e: IllegalArgumentException) {
        println("rejected: ${e.message}")
    }
}
