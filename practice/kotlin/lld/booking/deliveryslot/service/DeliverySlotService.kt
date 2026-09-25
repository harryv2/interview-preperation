package lld.booking.deliveryslot.service

import lld.booking.deliveryslot.entity.Booking
import lld.booking.deliveryslot.entity.BookingStatus
import lld.booking.deliveryslot.entity.Order
import lld.booking.deliveryslot.entity.TimeSlot
import lld.booking.deliveryslot.entity.Warehouse
import lld.booking.deliveryslot.exception.BookingNotFoundException
import lld.booking.deliveryslot.exception.InvalidSlotException
import lld.booking.deliveryslot.exception.NoServiceableWarehouseException
import lld.booking.deliveryslot.repository.BookingRepository
import lld.booking.deliveryslot.repository.OrderRepository
import lld.booking.deliveryslot.repository.WarehouseRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DeliverySlotService(
    private val warehouses: WarehouseRepository,
    private val orders: OrderRepository,
    private val bookings: BookingRepository,
    private val catalog: SlotCatalog,
    private val clock: Clock,
    private val cutoffHours: Long = 2
) {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    fun getAvailableSlots(order: Order): List<TimeSlot> {
        val warehouse = warehouseFor(order)
        val cutoff = cutoff()

        return catalog.slotsFor(warehouse.id)
            .filter { it.start.isAfter(cutoff) }
            .filter { warehouse.hasCapacityOn(it.id) }
    }

    fun bookSlot(order: Order, slotId: String): Booking {
        val warehouse = warehouseFor(order)

        bookings.findActiveByOrderId(order.id)?.let {
            throw IllegalStateException("Order ${order.id} already booked on slot ${it.slotId}")
        }

        val slot = catalog.findSlot(warehouse.id, slotId)
            ?: throw InvalidSlotException("Unknown slot $slotId")

        if (!slot.start.isAfter(cutoff())) {
            throw InvalidSlotException("Slot $slotId is past the cutoff")
        }

        lockFor(slotId).withLock {
            val van = warehouse.assignVan(slotId)
            return bookings.save(
                Booking(
                    id = UUID.randomUUID().toString(),
                    orderId = order.id,
                    slotId = slotId,
                    vanId = van.id,
                    warehouseId = warehouse.id,
                    status = BookingStatus.CONFIRMED
                )
            )
        }
    }

    fun cancelBooking(bookingId: String): Booking {
        val booking = bookings.findById(bookingId)
            ?: throw BookingNotFoundException("No booking $bookingId")

        if (booking.status == BookingStatus.CANCELLED) {
            return booking
        }

        val warehouse = warehouses.findById(booking.warehouseId)
            ?: throw NoServiceableWarehouseException("Unknown warehouse ${booking.warehouseId}")

        lockFor(booking.slotId).withLock {
            warehouse.releaseVan(booking.vanId, booking.slotId)
            return bookings.save(booking.copy(status = BookingStatus.CANCELLED))
        }
    }

    fun rescheduleBooking(bookingId: String, newSlotId: String): Booking {
        val old = bookings.findById(bookingId)
            ?: throw BookingNotFoundException("No booking $bookingId")
        val order = orders.findById(old.orderId)
            ?: throw IllegalStateException("No order ${old.orderId}")

        cancelBooking(bookingId)
        return try {
            bookSlot(order, newSlotId)
        } catch (e: Exception) {
            bookSlot(order, old.slotId)
            throw e
        }
    }

    private fun warehouseFor(order: Order): Warehouse {
        return warehouses.findByZip(order.zip)
            ?: throw NoServiceableWarehouseException("No warehouse serves zip ${order.zip}")
    }

    private fun cutoff(): LocalDateTime {
        return LocalDateTime.now(clock).plusHours(cutoffHours)
    }

    private fun lockFor(slotId: String): ReentrantLock {
        return locks.computeIfAbsent(slotId) { ReentrantLock() }
    }
}
