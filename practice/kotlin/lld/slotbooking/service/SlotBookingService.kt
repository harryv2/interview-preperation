package lld.slotbooking.service

import lld.slotbooking.entity.Booking
import lld.slotbooking.entity.BookingId
import lld.slotbooking.entity.OrderId
import lld.slotbooking.entity.Slot
import lld.slotbooking.entity.SlotId
import java.time.LocalDate

interface SlotBookingService {
    fun getAvailableSlots(zone: String, from: LocalDate, to: LocalDate): List<Slot>
    fun bookSlot(orderId: OrderId, slotId: SlotId): Booking
    fun cancelBooking(bookingId: BookingId): Booking
}
