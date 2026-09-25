package lld.commerce.amazonlockersimpledelivery.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class ShipmentStatus {
    PENDING,
    DELIVERED,
    PICKED_UP,
    EXPIRED,
}

data class Placement(
    val locker: Locker,
    val slot: LockerSlot,
    val pickupCode: String,
    val expiresAt: Instant,
)

class Shipment(
    val id: Uuid,
    val pkg: Package,
    val preferredLocker: Locker,
) {
    private val lock = ReentrantLock()

    @Volatile var status = ShipmentStatus.PENDING
        private set

    @Volatile var placement: Placement? = null
        private set

    fun markDelivered(placement: Placement) {
        lock.withLock {
            check(status == ShipmentStatus.PENDING) { "Shipment $id is $status, expected PENDING" }
            status = ShipmentStatus.DELIVERED
            this.placement = placement
        }
    }

    fun markPickedUp(now: Instant) {
        lock.withLock {
            check(status == ShipmentStatus.DELIVERED) { "Shipment $id is $status, expected DELIVERED" }
            check(now < placement!!.expiresAt) { "Pickup code for $id expired at ${placement!!.expiresAt}" }
            status = ShipmentStatus.PICKED_UP
        }
    }

    fun markExpired(now: Instant): Boolean {
        lock.withLock {
            if (status != ShipmentStatus.DELIVERED || now < placement!!.expiresAt) return false
            status = ShipmentStatus.EXPIRED
            return true
        }
    }
}
