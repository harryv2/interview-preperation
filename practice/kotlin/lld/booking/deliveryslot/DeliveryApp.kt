package lld.booking.deliveryslot

import lld.booking.deliveryslot.entity.Booking
import lld.booking.deliveryslot.entity.Customer
import lld.booking.deliveryslot.entity.DeliveryVan
import lld.booking.deliveryslot.entity.Order
import lld.booking.deliveryslot.entity.TimeSlot
import lld.booking.deliveryslot.entity.Warehouse
import lld.booking.deliveryslot.repository.InMemoryBookingRepository
import lld.booking.deliveryslot.repository.InMemoryCustomerRepository
import lld.booking.deliveryslot.repository.InMemoryOrderRepository
import lld.booking.deliveryslot.repository.InMemoryWarehouseRepository
import lld.booking.deliveryslot.service.DeliverySlotService
import lld.booking.deliveryslot.service.SlotCatalog
import lld.booking.deliveryslot.service.SlotConfig
import lld.booking.deliveryslot.strategy.LeastLoadedStrategy
import lld.booking.deliveryslot.strategy.VanAssignmentStrategy
import java.time.Clock

class DeliveryApp(clock: Clock = Clock.systemDefaultZone()) {
    private val customers = InMemoryCustomerRepository()
    private val orders = InMemoryOrderRepository()
    private val warehouses = InMemoryWarehouseRepository()
    private val bookings = InMemoryBookingRepository()
    private val catalog = SlotCatalog(SlotConfig(), clock)
    private val service = DeliverySlotService(warehouses, orders, bookings, catalog, clock)

    fun addCustomer(id: String, name: String, zip: Int): Customer {
        return customers.save(Customer(id, name, zip))
    }

    fun addWarehouse(
        id: String,
        vanCount: Int,
        vanCapacity: Int,
        zips: Set<Int>,
        strategy: VanAssignmentStrategy = LeastLoadedStrategy()
    ): Warehouse {
        val vans = (1..vanCount).map { DeliveryVan("$id-VAN-$it", "KA-01-$it", vanCapacity) }
        return warehouses.save(Warehouse(id, "Warehouse-$id", zips, vans, strategy))
    }

    fun createOrder(id: String, customerId: String): Order {
        val customer = customers.findById(customerId)
        requireNotNull(customer) { "No customer $customerId" }
        return orders.save(Order(id, customer.id, customer.zip))
    }

    fun getAvailableSlots(orderId: String): List<TimeSlot> {
        return service.getAvailableSlots(order(orderId))
    }

    fun bookSlot(orderId: String, slotId: String): Booking {
        return service.bookSlot(order(orderId), slotId)
    }

    fun cancelBooking(bookingId: String): Booking {
        return service.cancelBooking(bookingId)
    }

    fun rescheduleBooking(bookingId: String, newSlotId: String): Booking {
        return service.rescheduleBooking(bookingId, newSlotId)
    }

    private fun order(orderId: String): Order {
        val order = orders.findById(orderId)
        requireNotNull(order) { "No order $orderId" }
        return order
    }
}
