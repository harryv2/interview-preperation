package lld.slotbooking.service

import lld.slotbooking.entity.Booking
import lld.slotbooking.entity.BookingId
import lld.slotbooking.entity.BookingStatus
import lld.slotbooking.entity.OrderId
import lld.slotbooking.entity.Slot
import lld.slotbooking.entity.SlotId
import lld.slotbooking.exception.AlreadyBooked
import lld.slotbooking.exception.BookingNotFound
import lld.slotbooking.exception.SlotClosed
import lld.slotbooking.exception.SlotFull
import lld.slotbooking.exception.SlotNotFound
import lld.slotbooking.repository.BookingRepository
import lld.slotbooking.repository.SlotRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DefaultSlotBookingService(
    private val slots: SlotRepository,
    private val bookings: BookingRepository,
    private val clock: Clock,
    private val cutoff: Duration = Duration.ofHours(2)
) : SlotBookingService {

    override fun getAvailableSlots(zone: String, from: LocalDate, to: LocalDate): List<Slot> {
        val earliestStart = earliestStart()
        return slots.findByZone(zone, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
            .filter { it.start >= earliestStart && it.remaining > 0 }
            .sortedBy { it.start }
    }

    override fun bookSlot(orderId: OrderId, slotId: SlotId): Booking {
        bookings.findActiveByOrder(orderId)?.let {
            if (it.slotId == slotId) {
                return it
            }
            throw AlreadyBooked(orderId, it.slotId)
        }

        val slot = slots.find(slotId) ?: throw SlotNotFound(slotId)
        if (slot.start < earliestStart()) {
            throw SlotClosed(slotId)
        }
        if (!slots.tryReserve(slotId)) {
            throw SlotFull(slotId)
        }

        val booking = Booking(
            id = BookingId(UUID.randomUUID().toString()),
            orderId = orderId,
            slotId = slotId,
            status = BookingStatus.CONFIRMED,
            createdAt = clock.instant()
        )
        if (!bookings.tryInsert(booking)) {
            slots.release(slotId)
            return bookSlot(orderId, slotId)
        }
        return booking
    }

    override fun cancelBooking(bookingId: BookingId): Booking {
        val cancelled = bookings.tryCancel(bookingId)
        if (cancelled != null) {
            slots.release(cancelled.slotId)
            return cancelled
        }
        return bookings.find(bookingId) ?: throw BookingNotFound(bookingId)
    }

    private fun earliestStart(): LocalDateTime {
        return LocalDateTime.now(clock).plus(cutoff)
    }
}
