package lld.commerce.zomatooms.service

import lld.commerce.zomatooms.entity.Bill
import lld.commerce.zomatooms.entity.DeliveryPartner
import lld.commerce.zomatooms.entity.Money
import lld.commerce.zomatooms.entity.Order
import lld.commerce.zomatooms.entity.OrderItem
import lld.commerce.zomatooms.entity.OrderStatus
import lld.commerce.zomatooms.entity.Restaurant
import lld.commerce.zomatooms.strategy.DiscountStrategy
import lld.commerce.zomatooms.strategy.PartnerAssignmentStrategy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


data class ChargeConfig(
    val packagingFee: Money = Money.rupees(20),
    val deliveryFee: Money = Money.rupees(30),
    val taxPercent: Int = 5
)


class OrderService(
    private val restaurants: List<Restaurant>,
    private val partners: List<DeliveryPartner>,
    private val discountStrategy: DiscountStrategy,
    private val assignmentStrategy: PartnerAssignmentStrategy,
    private val charges: ChargeConfig = ChargeConfig()
) {

    private val orders = ConcurrentHashMap<String, Order>()
    private val assignmentLock = ReentrantLock()

    fun placeOrder(customerId: String, restaurantId: String, quantities: Map<String, Int>): Order {
        require(quantities.isNotEmpty()) { "Order must have at least one item" }

        val restaurant = restaurants.firstOrNull { it.id == restaurantId }
        requireNotNull(restaurant) { "No restaurant $restaurantId" }
        require(restaurant.isOpen) { "${restaurant.name} is closed" }

        val items = quantities.map { (itemId, quantity) ->
            require(quantity > 0) { "Quantity for $itemId must be positive" }
            val item = restaurant.find(itemId)
            requireNotNull(item) { "${restaurant.name} does not serve $itemId" }
            OrderItem(item, quantity)
        }

        val order = Order(
            id = UUID.randomUUID().toString().take(8),
            customerId = customerId,
            restaurantId = restaurantId,
            items = items,
            bill = billFor(items)
        )
        orders[order.id] = order
        return order
    }

    fun acceptOrder(orderId: String): Order {
        return move(orderId, OrderStatus.ACCEPTED)
    }

    fun rejectOrder(orderId: String): Order {
        return move(orderId, OrderStatus.REJECTED)
    }

    fun markReady(orderId: String): Order {
        return move(orderId, OrderStatus.READY)
    }

    fun assignPartner(orderId: String): DeliveryPartner {
        val order = order(orderId)
        require(order.status == OrderStatus.READY) { "Order $orderId is ${order.status}, not ready for pickup" }
        require(order.deliveryPartnerId == null) { "Order $orderId already has a partner" }

        assignmentLock.withLock {
            val partner = assignmentStrategy.pick(partners)
            requireNotNull(partner) { "No delivery partner is free" }
            check(partner.tryAssign(orderId)) { "Partner ${partner.id} was taken" }
            order.assignTo(partner.id)
            return partner
        }
    }

    fun pickUp(orderId: String): Order {
        val order = order(orderId)
        checkNotNull(order.deliveryPartnerId) { "Order $orderId has no delivery partner" }
        order.moveTo(OrderStatus.OUT_FOR_DELIVERY)
        return order
    }

    fun deliver(orderId: String): Order {
        val order = order(orderId)
        order.moveTo(OrderStatus.DELIVERED)
        freePartner(order.deliveryPartnerId, order.id)
        return order
    }

    fun cancelOrder(orderId: String): Order {
        val order = order(orderId)
        order.moveTo(OrderStatus.CANCELLED)
        freePartner(order.releasePartner(), order.id)
        return order
    }

    fun ordersOf(customerId: String): List<Order> {
        return orders.values.filter { it.customerId == customerId }
    }

    private fun billFor(items: List<OrderItem>): Bill {
        var itemTotal = Money.ZERO
        items.forEach {
            itemTotal += it.lineTotal
        }

        return Bill(
            itemTotal = itemTotal,
            packagingFee = charges.packagingFee,
            deliveryFee = charges.deliveryFee,
            tax = itemTotal.percent(charges.taxPercent),
            discount = discountStrategy.discountOn(itemTotal)
        )
    }

    private fun move(orderId: String, next: OrderStatus): Order {
        val order = order(orderId)
        order.moveTo(next)
        return order
    }

    private fun freePartner(partnerId: String?, orderId: String) {
        if (partnerId == null) {
            return
        }
        partners.firstOrNull { it.id == partnerId }?.release(orderId)
    }

    private fun order(orderId: String): Order {
        val order = orders[orderId]
        requireNotNull(order) { "No order $orderId" }
        return order
    }
}
