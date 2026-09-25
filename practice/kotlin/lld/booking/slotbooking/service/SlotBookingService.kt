package lld.booking.slotbooking.service

import lld.booking.slotbooking.entity.Booking
import lld.booking.slotbooking.entity.BookingId
import lld.booking.slotbooking.entity.OrderId
import lld.booking.slotbooking.entity.Slot
import lld.booking.slotbooking.entity.SlotId
import java.time.LocalDate

interface SlotBookingService {
    fun getAvailableSlots(zone: String, from: LocalDate, to: LocalDate): List<Slot>
    fun bookSlot(orderId: OrderId, slotId: SlotId): Booking
    fun cancelBooking(bookingId: BookingId): Booking
}
