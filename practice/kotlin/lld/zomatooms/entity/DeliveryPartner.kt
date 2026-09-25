package lld.zomatooms.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class DeliveryPartner(
    val id: String,
    val name: String
) {

    private val lock = ReentrantLock()
    private var currentOrderId: String? = null

    fun isFree(): Boolean {
        return lock.withLock { currentOrderId == null }
    }

    fun tryAssign(orderId: String): Boolean {
        lock.withLock {
            if (currentOrderId != null) {
                return false
            }
            currentOrderId = orderId
            return true
        }
    }

    fun release(orderId: String) {
        lock.withLock {
            if (currentOrderId == orderId) {
                currentOrderId = null
            }
        }
    }
}
