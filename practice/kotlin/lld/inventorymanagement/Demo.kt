package lld.inventorymanagement

import lld.inventorymanagement.service.InventoryService
import lld.inventorymanagement.strategy.MostStockFirstStrategy

fun main() {
    val inventory = InventoryService(MostStockFirstStrategy())
    inventory.addListener { product, warehouse, available ->
        println("  low stock: ${product.sku} at ${warehouse.id} has $available, reorder level ${product.reorderLevel}")
    }

    inventory.addProduct("SKU-1", "Widget", reorderLevel = 5)
    inventory.addWarehouse("BLR", "Bangalore")
    inventory.addWarehouse("DEL", "Delhi")

    inventory.receive("BLR", "SKU-1", 20)
    inventory.receive("DEL", "SKU-1", 3)
    println("stock -> ${inventory.stockOf("SKU-1")}")

    val r1 = inventory.reserve("O1", "SKU-1", 8)
    println("O1 reserve 8 -> ${r1?.warehouseId}, stock -> ${inventory.stockOf("SKU-1")}")

    val r2 = inventory.reserve("O2", "SKU-1", 15)
    println("O2 reserve 15 -> ${r2?.warehouseId}")

    inventory.confirm(r1!!.id)
    println("O1 confirmed, stock -> ${inventory.stockOf("SKU-1")}")

    val r3 = inventory.reserve("O3", "SKU-1", 4)
    inventory.release(r3!!.id)
    println("O3 reserved then released, stock -> ${inventory.stockOf("SKU-1")}")

    inventory.transfer("BLR", "DEL", "SKU-1", 5)
    println("transferred 5 BLR -> DEL, stock -> ${inventory.stockOf("SKU-1")}")

    inventory.ship("BLR", "SKU-1", 3)
    println("shipped 3 from BLR, stock -> ${inventory.stockOf("SKU-1")}")

    try {
        inventory.ship("BLR", "SKU-1", 10)
    } catch (e: IllegalStateException) {
        println("rejected: ${e.message}")
    }

    println("ledger:")
    inventory.movementsOf("SKU-1").forEach { println("  ${it.type} ${it.warehouseId} ${it.quantity}") }
}
