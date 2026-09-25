package lld.booking.eventbooking

import lld.booking.eventbooking.entity.Event
import lld.booking.eventbooking.entity.EventCategory
import lld.booking.eventbooking.entity.FixedClock
import lld.booking.eventbooking.entity.Money
import lld.booking.eventbooking.entity.Occurrence
import lld.booking.eventbooking.entity.OrderLine
import lld.booking.eventbooking.entity.Section
import lld.booking.eventbooking.entity.SeatedInventory
import lld.booking.eventbooking.entity.StandingInventory
import lld.booking.eventbooking.entity.TicketTier
import lld.booking.eventbooking.entity.Venue
import lld.booking.eventbooking.service.BookingService
import lld.booking.eventbooking.strategy.CardMethod
import lld.booking.eventbooking.strategy.DecliningCard
import lld.booking.eventbooking.strategy.DemandPricing
import lld.booking.eventbooking.strategy.FlatTierPricing
import lld.booking.eventbooking.strategy.UpiMethod
import java.time.Duration
import java.time.Instant

fun main() {

    val vipSection = Section.seated("S-VIP", "VIP Front", listOf("A", "B"), 4)
    val gaSection = Section.standing("S-GA", "General Admission", 20)
    val venue = Venue("V1", "Indira Gandhi Arena", "Delhi", listOf(vipSection, gaSection))

    val vipTier = TicketTier("T-VIP", "VIP", vipSection.id, Money.rupees(12000))
    val gaTier = TicketTier("T-GA", "GA", gaSection.id, Money.rupees(3000))

    val event = Event("E1", "Arijit Singh Live", EventCategory.CONCERT, maxTicketsPerUser = 6)
    val clock = FixedClock(Instant.parse("2026-11-01T10:00:00Z"))

    val occurrence = Occurrence(
        id = "OCC-1",
        event = event,
        venue = venue,
        startsAt = Instant.parse("2026-11-14T19:00:00Z"),
        inventories = listOf(
            SeatedInventory(vipTier, vipSection.seats),
            StandingInventory(gaTier, gaSection.capacity)
        )
    )

    val service = BookingService(
        occurrences = listOf(occurrence),
        pricing = FlatTierPricing(),
        clock = clock,
        holdWindow = Duration.ofMinutes(8)
    )

    banner("on sale")
    println("  $occurrence")
    println("  ${service.availability("OCC-1")}")

    banner("reserved seating, the buyer picks the seat")
    val aarav = service.startOrder("U-aarav", "OCC-1", listOf(
        OrderLine.seats("T-VIP", listOf("S-VIP-A1", "S-VIP-A2"))
    ))
    println("  $aarav")
    println("  ${service.pay(aarav.id, CardMethod(), "idem-aarav-1")}")

    banner("general admission, the buyer picks a count and nobody has a seat")
    val bhavna = service.startOrder("U-bhavna", "OCC-1", listOf(
        OrderLine.quantity("T-GA", 3)
    ))
    println("  $bhavna")
    println("  ${service.pay(bhavna.id, UpiMethod(), "idem-bhavna-1")}")
    println("  ${service.availability("OCC-1")}")

    banner("one order across both kinds of inventory")
    val chirag = service.startOrder("U-chirag", "OCC-1", listOf(
        OrderLine.seats("T-VIP", listOf("S-VIP-B1")),
        OrderLine.quantity("T-GA", 2)
    ))
    println("  $chirag")

    banner("a seat already held cannot be held again")
    attempt("someone else wants B1") {
        service.startOrder("U-divya", "OCC-1", listOf(OrderLine.seats("T-VIP", listOf("S-VIP-B1"))))
    }

    banner("all or nothing, the good line is rolled back when the bad one fails")
    println("  GA left before: ${service.availability("OCC-1")["GA"]}")
    attempt("2 free VIP seats plus 50 GA that do not exist") {
        service.startOrder("U-divya", "OCC-1", listOf(
            OrderLine.seats("T-VIP", listOf("S-VIP-A3", "S-VIP-A4")),
            OrderLine.quantity("T-GA", 50)
        ))
    }
    println("  A3 and A4 were not consumed: ${service.availability("OCC-1")}")

    banner("a declined card keeps the hold alive")
    val esha = service.startOrder("U-esha", "OCC-1", listOf(OrderLine.quantity("T-GA", 2)))
    println("  ${service.pay(esha.id, DecliningCard(), "idem-esha-1")}")
    println("  hold survives, retry on another rail: ${service.pay(esha.id, UpiMethod(), "idem-esha-2")}")

    banner("paying twice with the same key does not buy twice")
    println("  ${service.pay(esha.id, UpiMethod(), "idem-esha-2")}")

    banner("chirag walks away, the hold expires")
    println("  before: ${service.availability("OCC-1")}")
    clock.advance(Duration.ofMinutes(9))
    println("  reclaimed ${service.sweepExpired()} order(s)")
    println("  after:  ${service.availability("OCC-1")}")
    println("  $chirag")

    banner("per person cap of ${event.maxTicketsPerUser}")
    val farhan = service.startOrder("U-farhan", "OCC-1", listOf(OrderLine.quantity("T-GA", 5)))
    service.pay(farhan.id, CardMethod(), "idem-farhan-1")
    attempt("the same person comes back for 3 more") {
        service.startOrder("U-farhan", "OCC-1", listOf(OrderLine.quantity("T-GA", 3)))
    }

    banner("demand pricing moves with what the tier has given up")
    demandPricingDemo()

    banner("aarav's orders")
    service.ordersOf("U-aarav").forEach { println("  $it") }
}

private fun demandPricingDemo() {
    val pit = Section.standing("S-PIT", "Pit", 8)
    val venue = Venue("V2", "Phoenix Grounds", "Mumbai", listOf(pit))
    val tier = TicketTier("T-PIT", "Pit", pit.id, Money.rupees(2000))
    val event = Event("E2", "Indie Night", EventCategory.CONCERT, maxTicketsPerUser = 20)
    val clock = FixedClock(Instant.parse("2026-11-01T10:00:00Z"))

    val occurrence = Occurrence(
        id = "OCC-2",
        event = event,
        venue = venue,
        startsAt = Instant.parse("2026-12-01T20:00:00Z"),
        inventories = listOf(StandingInventory(tier, pit.capacity))
    )

    val service = BookingService(
        occurrences = listOf(occurrence),
        pricing = DemandPricing(stepPercent = 15),
        clock = clock
    )

    listOf("U-1", "U-2", "U-3").forEach { userId ->
        val order = service.startOrder(userId, "OCC-2", listOf(OrderLine.quantity("T-PIT", 2)))
        println("  $userId pays ${order.total} for 2, ${service.availability("OCC-2")["Pit"]} left")
        service.pay(order.id, CardMethod(), "idem-$userId")
    }
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
