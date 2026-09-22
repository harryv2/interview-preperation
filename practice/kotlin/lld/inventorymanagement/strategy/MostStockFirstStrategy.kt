package lld.inventorymanagement.strategy

import lld.inventorymanagement.entity.Warehouse

class MostStockFirstStrategy : WarehouseSelectionStrategy {
    override fun candidates(warehouses: List<Warehouse>, sku: String, quantity: Int): List<Warehouse> {
        return warehouses
            .filter { it.availableOf(sku) >= quantity }
            .sortedByDescending { it.availableOf(sku) }
    }
}
