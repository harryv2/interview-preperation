package lld.commerce.inventorymanagement.service

import lld.commerce.inventorymanagement.entity.Product
import lld.commerce.inventorymanagement.entity.Warehouse

fun interface LowStockListener {
    fun onLowStock(product: Product, warehouse: Warehouse, available: Int)
}
