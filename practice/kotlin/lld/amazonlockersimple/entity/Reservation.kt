package lld.amazonlockersimple.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class ReservationStatus {
    RESERVED,
    DELIVERED,
    PICKED_UP,
    EXPIRED,
}

class Reservation(
    val id: Uuid,
    val pkg: Package,
    val locker: Locker,
    val slot: LockerSlot,
) {
    private val lock = ReentrantLock()

    @Volatile var status = ReservationStatus.RESERVED
        private set

    @Volatile var pickupCode: String? = null
        private set

    @Volatile var expiresAt: Instant? = null
        private set

    fun markDelivered(pickupCode: String, expiresAt: Instant) {
        lock.withLock {
            check(status == ReservationStatus.RESERVED) { "Reservation $id is $status, expected RESERVED" }
            status = ReservationStatus.DELIVERED
            this.pickupCode = pickupCode
            this.expiresAt = expiresAt
        }
    }

    fun markPickedUp(now: Instant) {
        lock.withLock {
            check(status == ReservationStatus.DELIVERED) { "Reservation $id is $status, expected DELIVERED" }
            check(now < expiresAt!!) { "Pickup code for $id expired at $expiresAt" }
            status = ReservationStatus.PICKED_UP
        }
    }

    fun markExpired(now: Instant): Boolean {
        lock.withLock {
            if (status != ReservationStatus.DELIVERED || now < expiresAt!!) return false
            status = ReservationStatus.EXPIRED
            return true
        }
    }
}
