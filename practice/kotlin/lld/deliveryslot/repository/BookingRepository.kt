package lld.deliveryslot.repository

import lld.deliveryslot.entity.Booking
import lld.deliveryslot.entity.BookingStatus

interface BookingRepository {
    fun save(booking: Booking): Booking
    fun findById(id: String): Booking?
    fun findActiveByOrderId(orderId: String): Booking?
}

class InMemoryBookingRepository : BookingRepository {
    private val map = HashMap<String, Booking>()

    override fun save(booking: Booking): Booking {
        map[booking.id] = booking
        return booking
    }

    override fun findById(id: String): Booking? {
        return map[id]
    }

    override fun findActiveByOrderId(orderId: String): Booking? {
        return map.values.firstOrNull { it.orderId == orderId && it.status == BookingStatus.CONFIRMED }
    }
}
