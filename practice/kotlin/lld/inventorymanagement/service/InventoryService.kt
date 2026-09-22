package lld.inventorymanagement.service

import lld.inventorymanagement.entity.MovementType
import lld.inventorymanagement.entity.Product
import lld.inventorymanagement.entity.Reservation
import lld.inventorymanagement.entity.StockItem
import lld.inventorymanagement.entity.StockMovement
import lld.inventorymanagement.entity.Warehouse
import lld.inventorymanagement.strategy.WarehouseSelectionStrategy
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class InventoryService(
    private val strategy: WarehouseSelectionStrategy,
    private val clock: Clock = Clock.systemUTC()
) {
    private val products = ConcurrentHashMap<String, Product>()
    private val warehouses = ConcurrentHashMap<String, Warehouse>()
    private val reservations = ConcurrentHashMap<String, Reservation>()
    private val listeners = mutableListOf<LowStockListener>()

    private val ledgerLock = ReentrantLock()
    private val ledger = mutableListOf<StockMovement>()

    fun addProduct(sku: String, name: String, reorderLevel: Int): Product {
        val product = Product(sku, name, reorderLevel)
        products[sku] = product
        return product
    }

    fun addWarehouse(id: String, name: String): Warehouse {
        val warehouse = Warehouse(id, name)
        warehouses[id] = warehouse
        return warehouse
    }

    fun addListener(listener: LowStockListener) {
        listeners.add(listener)
    }

    fun receive(warehouseId: String, sku: String, quantity: Int) {
        val warehouse = warehouse(warehouseId)
        warehouse.item(product(sku)).add(quantity)
        record(MovementType.RECEIVED, sku, warehouseId, quantity)
    }

    fun ship(warehouseId: String, sku: String, quantity: Int) {
        val warehouse = warehouse(warehouseId)
        val item = warehouse.item(product(sku))
        item.remove(quantity)
        record(MovementType.SHIPPED, sku, warehouseId, quantity)
        checkLowStock(item, warehouse)
    }

    fun reserve(orderId: String, sku: String, quantity: Int): Reservation? {
        val product = product(sku)

        for (warehouse in strategy.candidates(warehouses.values.toList(), sku, quantity)) {
            val item = warehouse.item(product)

            if (item.tryReserve(quantity)) {
                val reservation = Reservation(UUID.randomUUID().toString(), orderId, sku, warehouse.id, quantity)
                reservations[reservation.id] = reservation
                record(MovementType.RESERVED, sku, warehouse.id, quantity)
                checkLowStock(item, warehouse)
                return reservation
            }
        }

        return null
    }

    fun confirm(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.confirm()
        item(reservation).commit(reservation.quantity)
        record(MovementType.SHIPPED, reservation.sku, reservation.warehouseId, reservation.quantity)
        return reservation
    }

    fun release(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.release()
        item(reservation).release(reservation.quantity)
        record(MovementType.RELEASED, reservation.sku, reservation.warehouseId, reservation.quantity)
        return reservation
    }

    fun transfer(fromWarehouseId: String, toWarehouseId: String, sku: String, quantity: Int) {
        val product = product(sku)
        val from = warehouse(fromWarehouseId)
        val to = warehouse(toWarehouseId)

        val source = from.item(product)
        source.remove(quantity)
        record(MovementType.TRANSFER_OUT, sku, from.id, quantity)

        to.item(product).add(quantity)
        record(MovementType.TRANSFER_IN, sku, to.id, quantity)

        checkLowStock(source, from)
    }

    fun stockOf(sku: String): Map<String, Int> {
        return warehouses.values.associate { it.id to it.availableOf(sku) }
    }

    fun movementsOf(sku: String): List<StockMovement> {
        ledgerLock.withLock {
            return ledger.filter { it.sku == sku }
        }
    }

    private fun record(type: MovementType, sku: String, warehouseId: String, quantity: Int) {
        ledgerLock.withLock {
            ledger.add(StockMovement(type, sku, warehouseId, quantity, clock.instant()))
        }
    }

    private fun checkLowStock(item: StockItem, warehouse: Warehouse) {
        val available = item.available()
        if (available <= item.product.reorderLevel) {
            listeners.forEach { it.onLowStock(item.product, warehouse, available) }
        }
    }

    private fun product(sku: String): Product {
        val product = products[sku]
        requireNotNull(product) { "Product $sku not found" }
        return product
    }

    private fun warehouse(id: String): Warehouse {
        val warehouse = warehouses[id]
        requireNotNull(warehouse) { "Warehouse $id not found" }
        return warehouse
    }

    private fun reservation(id: String): Reservation {
        val reservation = reservations[id]
        requireNotNull(reservation) { "Reservation $id not found" }
        return reservation
    }

    private fun item(reservation: Reservation): StockItem {
        val item = warehouse(reservation.warehouseId).find(reservation.sku)
        requireNotNull(item) { "No stock item for ${reservation.sku} at ${reservation.warehouseId}" }
        return item
    }
}
