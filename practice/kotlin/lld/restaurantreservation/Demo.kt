package lld.restaurantreservation

import lld.restaurantreservation.entity.Customer
import lld.restaurantreservation.entity.Table
import lld.restaurantreservation.service.Restaurant
import lld.restaurantreservation.strategy.SmallestFitStrategy
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

    val tomorrow = LocalDate.now().plusDays(1)
    val seven = tomorrow.atTime(LocalTime.of(19, 0))
    val eight = tomorrow.atTime(LocalTime.of(20, 0))
    val nine = tomorrow.atTime(LocalTime.of(21, 0))

    println("free for 3 at 19:00 -> ${restaurant.availableTables(3, seven).map { it.id }}")

    val r1 = restaurant.reserve(asha, 3, seven)
    println("asha 3 @19:00 -> ${r1?.table?.id}")

    val r2 = restaurant.reserve(ravi, 3, seven)
    println("ravi 3 @19:00 -> ${r2?.table?.id}")

    val r3 = restaurant.reserve(ravi, 3, eight)
    println("ravi 3 @20:00 -> ${r3?.table?.id}")

    val r4 = restaurant.reserve(asha, 3, nine)
    println("asha 3 @21:00 -> ${r4?.table?.id}")

    val r5 = restaurant.reserve(asha, 10, seven)
    println("asha 10 @19:00 -> ${r5?.table?.id}")

    restaurant.cancel(r1!!.id)
    println("cancelled asha @19:00, free for 3 -> ${restaurant.availableTables(3, seven).map { it.id }}")

    restaurant.checkIn(r2!!.id)
    restaurant.complete(r2.id)
    println("ravi @19:00 -> ${r2.status}, free for 3 -> ${restaurant.availableTables(3, seven).map { it.id }}")

    println("asha's reservations -> ${restaurant.reservationsOf(asha).map { "${it.table.id}:${it.status}" }}")

    try {
        restaurant.reserve(asha, 2, tomorrow.atTime(LocalTime.of(22, 30)))
    } catch (e: IllegalArgumentException) {
        println("rejected: ${e.message}")
    }
}
