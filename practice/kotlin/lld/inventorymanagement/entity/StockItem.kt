package lld.inventorymanagement.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class StockItem(
    val product: Product
) {
    private val lock = ReentrantLock()

    var onHand: Int = 0
        private set

    var reserved: Int = 0
        private set

    fun available(): Int {
        lock.withLock {
            return onHand - reserved
        }
    }

    fun add(quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        lock.withLock {
            onHand += quantity
        }
    }

    fun remove(quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        lock.withLock {
            check(quantity <= onHand - reserved) { "Insufficient stock for ${product.sku}" }
            onHand -= quantity
        }
    }

    fun tryReserve(quantity: Int): Boolean {
        require(quantity > 0) { "quantity must be positive" }
        lock.withLock {
            if (quantity > onHand - reserved) {
                return false
            }

            reserved += quantity
            return true
        }
    }

    fun release(quantity: Int) {
        lock.withLock {
            check(quantity <= reserved) { "Releasing more than reserved for ${product.sku}" }
            reserved -= quantity
        }
    }

    fun commit(quantity: Int) {
        lock.withLock {
            check(quantity <= reserved) { "Committing more than reserved for ${product.sku}" }
            reserved -= quantity
            onHand -= quantity
        }
    }
}
