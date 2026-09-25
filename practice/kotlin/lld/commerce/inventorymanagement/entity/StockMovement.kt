package lld.commerce.inventorymanagement.entity

import java.time.Instant

enum class MovementType {
    RECEIVED,
    SHIPPED,
    RESERVED,
    RELEASED,
    TRANSFER_OUT,
    TRANSFER_IN
}

class StockMovement(
    val type: MovementType,
    val sku: String,
    val warehouseId: String,
    val quantity: Int,
    val at: Instant
)
