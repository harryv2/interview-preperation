package lld.inventorymanagement.strategy

import lld.inventorymanagement.entity.Warehouse

interface WarehouseSelectionStrategy {
    fun candidates(warehouses: List<Warehouse>, sku: String, quantity: Int): List<Warehouse>
}
