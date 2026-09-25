package lld.hotel

import lld.hotel.entity.DateRange
import lld.hotel.entity.Guest
import lld.hotel.entity.Hotel
import lld.hotel.entity.Room
import lld.hotel.entity.RoomType
import lld.hotel.strategy.FlatRate
import lld.hotel.strategy.WeekendSurcharge
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun main() {

    // pinned so the arrival day rules below read the same on every run
    val clock = Clock.fixed(Instant.parse("2026-10-09T09:00:00Z"), ZoneOffset.UTC)

    val hotel = Hotel(
        id = "TAJ",
        name = "Taj Riverside",
        city = "Pune",
        rooms = listOf(
            Room("101", RoomType.SINGLE),
            Room("102", RoomType.SINGLE),
            Room("201", RoomType.DOUBLE),
            Room("202", RoomType.DOUBLE),
            Room("301", RoomType.SUITE)
        ),
        pricing = WeekendSurcharge(percent = 40),
        clock = clock
    )

    val aarav = Guest("g1", "Aarav")
    val bela = Guest("g2", "Bela")
    val chirag = Guest("g3", "Chirag")

    val friToTue = DateRange(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-13"))
    val tueToFri = DateRange(LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-16"))
    val straddling = DateRange(LocalDate.parse("2026-10-11"), LocalDate.parse("2026-10-14"))

    println(hotel)
    println("  free for $friToTue: ${hotel.availability(friToTue)}")

    println("\n== the same stay, two pricing policies ==")
    println("  flat rate:    ${FlatRate().quote(RoomType.DOUBLE, friToTue)}")
    println("  weekend +40%: ${hotel.quote(RoomType.DOUBLE, friToTue)}  (Fri and Sat cost more)")

    println("\n== book a type, not a room ==")
    val first = hotel.book(aarav, RoomType.DOUBLE, friToTue)
    val second = hotel.book(bela, RoomType.DOUBLE, friToTue)
    println("  $first")
    println("  $second")
    println("  doubles free for $friToTue: ${hotel.freeRooms(RoomType.DOUBLE, friToTue)}")

    println("\n== both doubles are committed for those nights ==")
    attempt("a third double, same nights") { hotel.book(chirag, RoomType.DOUBLE, friToTue) }

    println("\n== back to back is fine, the 13th is a checkout and a checkin ==")
    val third = hotel.book(chirag, RoomType.DOUBLE, tueToFri)
    println("  $third")

    println("\n== a straddling stay is refused on its busiest night ==")
    println("  nights 11 and 12 already have both doubles, night 13 has one")
    attempt("a double for $straddling") { hotel.book(chirag, RoomType.DOUBLE, straddling) }

    println("\n== a key is only handed over on the arrival day ==")
    attempt("check in Chirag on the 9th for a stay starting the 13th") { hotel.checkIn(third.id) }

    println("\n== the hotel hands out the room at check in ==")
    println("  ${hotel.checkIn(first.id)}")
    println("  ${hotel.checkIn(second.id)}")
    println("  room 201 now holds ${hotel.rooms().first { it.number == "201" }.stays()}")

    println("\n== lifecycle ==")
    println("  ${hotel.checkOut(first.id)}")
    attempt("check in again") { hotel.checkIn(first.id) }
    attempt("cancel a finished stay") { hotel.cancel(first.id) }

    println("\n== cancelling before arrival frees the inventory, no room was ever held ==")
    println("  doubles free for $tueToFri before: ${hotel.freeRooms(RoomType.DOUBLE, tueToFri)}")
    hotel.cancel(third.id)
    println("  after:  ${hotel.freeRooms(RoomType.DOUBLE, tueToFri)}")

    println("\n== rejected up front ==")
    attempt("check out before check in") {
        DateRange(LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-09"))
    }
    attempt("a zero night stay") {
        DateRange(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-09"))
    }

    println("\n== where things stand ==")
    println("  $friToTue -> ${hotel.availability(friToTue)}")
    println("  $tueToFri -> ${hotel.availability(tueToFri)}")
    println("  Chirag: ${hotel.reservationsOf(chirag)}")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
