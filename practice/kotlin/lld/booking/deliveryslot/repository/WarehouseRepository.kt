package lld.booking.deliveryslot.repository

import lld.booking.deliveryslot.entity.Warehouse

interface WarehouseRepository {
    fun save(warehouse: Warehouse): Warehouse
    fun findById(id: String): Warehouse?
    fun findByZip(zip: Int): Warehouse?
}

class InMemoryWarehouseRepository : WarehouseRepository {
    private val map = HashMap<String, Warehouse>()

    override fun save(warehouse: Warehouse): Warehouse {
        map[warehouse.id] = warehouse
        return warehouse
    }

    override fun findById(id: String): Warehouse? {
        return map[id]
    }

    override fun findByZip(zip: Int): Warehouse? {
        return map.values.firstOrNull { it.serves(zip) }
    }
}
