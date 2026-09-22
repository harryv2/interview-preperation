package lld.slotbooking.exception

import lld.slotbooking.entity.BookingId
import lld.slotbooking.entity.OrderId
import lld.slotbooking.entity.SlotId

class SlotNotFound(id: SlotId) : RuntimeException("slot ${id.value} not found")

class SlotClosed(id: SlotId) : RuntimeException("slot ${id.value} past booking cutoff")

class SlotFull(id: SlotId) : RuntimeException("slot ${id.value} has no capacity left")

class AlreadyBooked(orderId: OrderId, slotId: SlotId) :
    RuntimeException("order ${orderId.value} already booked on slot ${slotId.value}")

class BookingNotFound(id: BookingId) : RuntimeException("booking ${id.value} not found")
