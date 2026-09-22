package lld.deliveryslot.repository

import lld.deliveryslot.entity.Order

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: String): Order?
}

class InMemoryOrderRepository : OrderRepository {
    private val map = HashMap<String, Order>()

    override fun save(order: Order): Order {
        map[order.id] = order
        return order
    }

    override fun findById(id: String): Order? {
        return map[id]
    }
}
