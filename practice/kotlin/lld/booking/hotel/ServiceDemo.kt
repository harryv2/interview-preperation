package lld.booking.hotel

import lld.booking.hotel.entity.DateRange
import lld.booking.hotel.entity.Guest
import lld.booking.hotel.entity.Hotel
import lld.booking.hotel.entity.Room
import lld.booking.hotel.entity.RoomType
import lld.booking.hotel.service.BookingService
import lld.booking.hotel.strategy.FlatRate
import lld.booking.hotel.strategy.WeekendSurcharge
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun main() {

    val clock = Clock.fixed(Instant.parse("2026-10-09T09:00:00Z"), ZoneOffset.UTC)

    val taj = Hotel(
        id = "TAJ",
        name = "Taj Riverside",
        city = "Pune",
        rooms = listOf(
            Room("101", RoomType.SINGLE),
            Room("201", RoomType.DOUBLE),
            Room("202", RoomType.DOUBLE)
        ),
        pricing = WeekendSurcharge(percent = 40),
        clock = clock
    )

    val oberoi = Hotel(
        id = "OBE",
        name = "Oberoi Central",
        city = "Pune",
        rooms = listOf(
            Room("11", RoomType.DOUBLE),
            Room("12", RoomType.SUITE)
        ),
        pricing = FlatRate(),
        clock = clock
    )

    val leela = Hotel(
        id = "LEE",
        name = "Leela Bayside",
        city = "Mumbai",
        rooms = listOf(
            Room("501", RoomType.DOUBLE)
        ),
        pricing = FlatRate(),
        clock = clock
    )

    val service = BookingService()
    service.register(taj)
    service.register(oberoi)
    service.register(leela)

    val aarav = Guest("g1", "Aarav")
    val bela = Guest("g2", "Bela")

    val friToTue = DateRange(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-13"))

    println("== the platform, not a property ==")
    println("  Pune:   ${service.hotelsIn("Pune")}")
    println("  Mumbai: ${service.hotelsIn("Mumbai")}")

    println("\n== search is one question asked of every property in the city ==")
    println("  doubles for $friToTue")
    service.search("Pune", RoomType.DOUBLE, friToTue).forEach { println("    $it") }
    println("  the flat rate hotel wins because four nights includes a Friday and a Saturday")

    println("\n== a hotel with no room of that type never shows up ==")
    service.search("Pune", RoomType.SUITE, friToTue).forEach { println("    $it") }

    println("\n== booking goes to one property, which decides under its own lock ==")
    val first = service.book(aarav, "OBE", RoomType.DOUBLE, friToTue)
    println("  $first")
    println("  Pune doubles now: ${service.search("Pune", RoomType.DOUBLE, friToTue)}")

    println("\n== the offer was a snapshot, the hotel is the one that says no ==")
    attempt("a second double at Oberoi, same nights") {
        service.book(bela, "OBE", RoomType.DOUBLE, friToTue)
    }

    println("\n== a reservation says which property it belongs to, " + first.hotelId + " ==")
    println("  ${service.checkIn(first.id)}")
    println("  ${service.checkOut(first.id)}")

    println("\n== one guest, stays across properties ==")
    val second = service.book(aarav, "TAJ", RoomType.DOUBLE, friToTue)
    val third = service.book(aarav, "LEE", RoomType.DOUBLE, friToTue)
    service.reservationsOf(aarav).forEach { println("    $it") }
    println("  nothing stops the same guest holding two rooms on the same night, see the README")

    println("\n== rejected up front ==")
    attempt("book a hotel that does not exist") {
        service.book(bela, "HYA", RoomType.DOUBLE, friToTue)
    }
    attempt("check in an id nobody sold") { service.checkIn("TAJ-000000") }
    attempt("register TAJ twice") { service.register(taj) }

    println("\n  $second\n  $third")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
