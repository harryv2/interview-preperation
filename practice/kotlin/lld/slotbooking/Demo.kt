package lld.slotbooking

import lld.slotbooking.entity.OrderId
import lld.slotbooking.entity.Slot
import lld.slotbooking.entity.SlotId
import lld.slotbooking.exception.AlreadyBooked
import lld.slotbooking.exception.SlotFull
import lld.slotbooking.repository.InMemoryBookingRepository
import lld.slotbooking.repository.InMemorySlotRepository
import lld.slotbooking.service.DefaultSlotBookingService
import java.time.Clock
import java.time.LocalDate

fun main() {
    val clock = Clock.systemDefaultZone()
    val slots = InMemorySlotRepository()
    val service = DefaultSlotBookingService(slots, InMemoryBookingRepository(), clock)

    val tomorrow = LocalDate.now(clock).plusDays(1)
    for (hour in listOf(9, 11, 13)) {
        slots.add(
            Slot(
                id = SlotId("BLR-$tomorrow-$hour"),
                zone = "BLR",
                start = tomorrow.atTime(hour, 0),
                end = tomorrow.atTime(hour + 2, 0),
                capacity = 2,
                booked = 0
            )
        )
    }

    val available = service.getAvailableSlots("BLR", tomorrow, tomorrow)
    println("Available: ${available.map { "${it.id.value} (${it.remaining} left)" }}")

    val first = available.first().id
    val b1 = service.bookSlot(OrderId("O1"), first)
    println("O1 -> ${b1.slotId.value}")

    val again = service.bookSlot(OrderId("O1"), first)
    println("O1 again -> same booking: ${again.id == b1.id}")

    try {
        service.bookSlot(OrderId("O1"), available[1].id)
    } catch (e: AlreadyBooked) {
        println("O1 other slot -> ${e.message}")
    }

    service.bookSlot(OrderId("O2"), first)
    try {
        service.bookSlot(OrderId("O3"), first)
    } catch (e: SlotFull) {
        println("O3 -> ${e.message}")
    }

    service.cancelBooking(b1.id)
    val b3 = service.bookSlot(OrderId("O3"), first)
    println("after cancel, O3 -> ${b3.slotId.value}")
    println("Available: ${service.getAvailableSlots("BLR", tomorrow, tomorrow).map { "${it.id.value} (${it.remaining} left)" }}")
}
