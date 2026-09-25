package lld.commerce.inventorymanagement.strategy

import lld.commerce.inventorymanagement.entity.Warehouse

interface WarehouseSelectionStrategy {
    fun candidates(warehouses: List<Warehouse>, sku: String, quantity: Int): List<Warehouse>
}
