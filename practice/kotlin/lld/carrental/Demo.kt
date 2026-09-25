package lld.carrental

import lld.carrental.entity.Branch
import lld.carrental.entity.Car
import lld.carrental.entity.CarType
import lld.carrental.entity.Money
import lld.carrental.entity.TimeRange
import lld.carrental.service.RentalService
import lld.carrental.strategy.FirstAvailableCar
import lld.carrental.strategy.PerDayPricing
import java.time.LocalDateTime

fun main() {

    val blr = Branch(
        id = "BLR",
        name = "Bangalore Indiranagar",
        cars = listOf(
            Car("C1", "Swift", CarType.HATCHBACK),
            Car("C2", "City", CarType.SEDAN),
            Car("C3", "Creta", CarType.SUV)
        )
    )

    val hyd = Branch(
        id = "HYD",
        name = "Hyderabad Gachibowli",
        cars = listOf(
            Car("C4", "Swift", CarType.HATCHBACK)
        )
    )

    val service = RentalService(
        branches = listOf(blr, hyd),
        pricingStrategy = PerDayPricing(
            mapOf(
                CarType.HATCHBACK to Money.rupees(1500),
                CarType.SEDAN to Money.rupees(2500),
                CarType.SUV to Money.rupees(4000)
            )
        ),
        selectionStrategy = FirstAvailableCar()
    )

    val monday = LocalDateTime.of(2026, 10, 5, 9, 0)
    val wednesday = TimeRange(monday, monday.plusDays(2))
    val tuesday = TimeRange(monday.plusDays(1), monday.plusDays(3))
    val nextWeek = TimeRange(monday.plusDays(7), monday.plusDays(9))

    banner("search and reserve")
    println("  hatchbacks at BLR: ${service.search("BLR", CarType.HATCHBACK, wednesday)}")
    val first = service.reserve("U1", "BLR", CarType.HATCHBACK, wednesday)
    println("  $first")

    banner("the only hatchback is taken for an overlapping range")
    println("  search now: ${service.search("BLR", CarType.HATCHBACK, tuesday)}")
    attempt("reserve an overlapping range") {
        service.reserve("U2", "BLR", CarType.HATCHBACK, tuesday)
    }
    println("  but a later range is fine: ${service.reserve("U2", "BLR", CarType.HATCHBACK, nextWeek)}")

    banner("cancel puts the car back")
    service.cancel(first.id)
    println("  $first")
    println("  ${service.reserve("U3", "BLR", CarType.HATCHBACK, tuesday)}")

    banner("pickup and a late dropoff")
    val suv = service.reserve("U1", "BLR", CarType.SUV, wednesday)
    println("  quoted ${suv.quote}")
    service.pickUp(suv.id)
    println("  ${service.dropOff(suv.id, wednesday.end.plusDays(1))}")

    banner("rejected")
    attempt("pick up a cancelled reservation") {
        service.pickUp(first.id)
    }
    attempt("reserve a type the branch does not stock") {
        service.reserve("U1", "HYD", CarType.SUV, wednesday)
    }
    attempt("reserve at a branch that does not exist") {
        service.reserve("U1", "DEL", CarType.SUV, wednesday)
    }
    attempt("an end before its start") {
        TimeRange(monday.plusDays(2), monday)
    }

    banner("history for U1")
    service.reservationsOf("U1").forEach { println("  $it") }
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
