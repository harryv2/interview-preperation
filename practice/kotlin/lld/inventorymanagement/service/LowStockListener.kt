package lld.inventorymanagement.service

import lld.inventorymanagement.entity.Product
import lld.inventorymanagement.entity.Warehouse

fun interface LowStockListener {
    fun onLowStock(product: Product, warehouse: Warehouse, available: Int)
}
