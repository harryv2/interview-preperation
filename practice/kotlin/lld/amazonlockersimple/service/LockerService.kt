package lld.amazonlockersimple.service

import lld.amazonlockersimple.entity.Location
import lld.amazonlockersimple.entity.Locker
import lld.amazonlockersimple.entity.Package
import lld.amazonlockersimple.entity.Reservation
import lld.amazonlockersimple.entity.Size
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

class LockerService(
    lockers: List<Locker>,
    private val notifications: NotificationService = ConsoleNotificationService(),
    private val pickupWindow: Duration = 3.days,
    private val clock: Clock = Clock.System,
) {
    private val lockers = lockers.associateBy { it.id }
    private val reservations = ConcurrentHashMap<Uuid, Reservation>()
    private val byCode = ConcurrentHashMap<String, Reservation>()

    fun nearbyLockers(from: Location, size: Size, limit: Int = 5): List<Locker> {
        return lockers.values
            .filter { it.hasFreeSlot(size) }
            .sortedBy { it.location.distanceKm(from) }
            .take(limit)
    }

    fun reserve(pkg: Package, lockerId: String): Reservation? {
        val locker = requireNotNull(lockers[lockerId]) { "Locker $lockerId not found" }
        val slot = locker.reserve(pkg) ?: return null

        val reservation = Reservation(Uuid.random(), pkg, locker, slot)
        reservations[reservation.id] = reservation
        return reservation
    }

    fun deliver(reservationId: Uuid): Reservation {
        val reservation = find(reservationId)
        val code = issuePickupCode(reservation)
        runCatching { reservation.markDelivered(code, clock.now() + pickupWindow) }
            .onFailure { byCode.remove(codeKey(reservation.locker.id, code)) }
            .getOrThrow()
        reservation.slot.occupy()

        notifications.notify(
            reservation.pkg.customer,
            "Order ${reservation.pkg.orderId} is in locker ${reservation.locker.id} slot ${reservation.slot.id}. " +
                "Pickup code $code, valid till ${reservation.expiresAt}",
        )
        return reservation
    }

    fun pickup(lockerId: String, code: String): Reservation {
        val reservation = requireNotNull(byCode[codeKey(lockerId, code)]) { "Invalid pickup code for locker $lockerId" }
        reservation.markPickedUp(clock.now())
        release(reservation)
        return reservation
    }

    fun expireOverdue(): List<Reservation> {
        val now = clock.now()
        return reservations.values
            .filter { it.markExpired(now) }
            .onEach {
                release(it)
                notifications.notify(it.pkg.customer, "Order ${it.pkg.orderId} was not picked up and has been returned")
            }
    }

    private fun find(reservationId: Uuid): Reservation {
        return requireNotNull(reservations[reservationId]) { "Reservation $reservationId not found" }
    }

    private fun issuePickupCode(reservation: Reservation): String {
        while (true) {
            val code = Random.nextInt(100_000, 1_000_000).toString()
            if (byCode.putIfAbsent(codeKey(reservation.locker.id, code), reservation) == null) {
                return code
            }
        }
    }

    private fun release(reservation: Reservation) {
        reservation.slot.release()
        reservation.pickupCode?.let { byCode.remove(codeKey(reservation.locker.id, it)) }
    }

    private fun codeKey(lockerId: String, code: String): String {
        return "$lockerId:$code"
    }
}
