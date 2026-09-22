package lld.slotbooking.repository

import lld.slotbooking.entity.Booking
import lld.slotbooking.entity.BookingId
import lld.slotbooking.entity.BookingStatus
import lld.slotbooking.entity.OrderId
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface BookingRepository {
    fun find(id: BookingId): Booking?
    fun findActiveByOrder(orderId: OrderId): Booking?
    fun tryInsert(booking: Booking): Boolean
    fun tryCancel(id: BookingId): Booking?
}

class InMemoryBookingRepository : BookingRepository {
    private val lock = ReentrantLock()
    private val byId = HashMap<BookingId, Booking>()
    private val activeByOrder = HashMap<OrderId, Booking>()

    override fun find(id: BookingId): Booking? {
        return lock.withLock { byId[id] }
    }

    override fun findActiveByOrder(orderId: OrderId): Booking? {
        return lock.withLock { activeByOrder[orderId] }
    }

    override fun tryInsert(booking: Booking): Boolean {
        lock.withLock {
            if (booking.orderId in activeByOrder) {
                return false
            }
            byId[booking.id] = booking
            activeByOrder[booking.orderId] = booking
            return true
        }
    }

    override fun tryCancel(id: BookingId): Booking? {
        lock.withLock {
            val current = byId[id] ?: return null
            if (current.status != BookingStatus.CONFIRMED) {
                return null
            }
            val cancelled = current.copy(status = BookingStatus.CANCELLED)
            byId[id] = cancelled
            activeByOrder.remove(current.orderId)
            return cancelled
        }
    }
}
