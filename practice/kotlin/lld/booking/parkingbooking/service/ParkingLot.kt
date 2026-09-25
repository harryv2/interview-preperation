package lld.booking.parkingbooking.service

import lld.booking.parkingbooking.entity.Booking
import lld.booking.parkingbooking.entity.Company
import lld.booking.parkingbooking.entity.Slot
import lld.booking.parkingbooking.entity.SlotType
import lld.booking.parkingbooking.entity.TimeInterval
import lld.booking.parkingbooking.strategy.SlotSelectionStrategy
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class ParkingLot(
    val slots: List<Slot>,
    val strategy: SlotSelectionStrategy
) {
    private val bookings = ConcurrentHashMap<Uuid, Booking>()

    fun availableSlots(type: SlotType, interval: TimeInterval): List<Slot> {
        return strategy.candidates(slots, type, interval)
    }

    fun book(company: Company, type: SlotType, interval: TimeInterval): Booking? {
        for (slot in strategy.candidates(slots, type, interval)) {
            val booking = Booking(Uuid.random(), company, slot, interval)

            if (slot.tryBook(booking)) {
                bookings[booking.id] = booking
                return booking
            }
        }

        return null
    }

    fun cancel(bookingId: Uuid) {
        val booking = bookings.remove(bookingId)
        requireNotNull(booking) { "Booking $bookingId not found" }

        booking.slot.release(booking)
        booking.cancel()
    }

    fun bookingsOf(company: Company): List<Booking> {
        return bookings.values.filter { it.company.id == company.id }
    }
}
