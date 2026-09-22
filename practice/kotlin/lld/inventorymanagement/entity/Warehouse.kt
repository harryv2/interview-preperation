package lld.inventorymanagement.entity

import java.util.concurrent.ConcurrentHashMap

class Warehouse(
    val id: String,
    val name: String
) {
    private val items = ConcurrentHashMap<String, StockItem>()

    fun item(product: Product): StockItem {
        return items.computeIfAbsent(product.sku) { StockItem(product) }
    }

    fun find(sku: String): StockItem? {
        return items[sku]
    }

    fun availableOf(sku: String): Int {
        return items[sku]?.available() ?: 0
    }
}
