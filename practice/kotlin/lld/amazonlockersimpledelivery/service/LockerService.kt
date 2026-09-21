package lld.amazonlockersimpledelivery.service

import lld.amazonlockersimpledelivery.entity.Location
import lld.amazonlockersimpledelivery.entity.Locker
import lld.amazonlockersimpledelivery.entity.Package
import lld.amazonlockersimpledelivery.entity.Placement
import lld.amazonlockersimpledelivery.entity.Shipment
import lld.amazonlockersimpledelivery.entity.ShipmentStatus
import lld.amazonlockersimpledelivery.entity.Size
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
    private val shipments = ConcurrentHashMap<Uuid, Shipment>()
    private val byCode = ConcurrentHashMap<String, Shipment>()

    fun nearbyLockers(from: Location, size: Size, limit: Int = 5): List<Locker> {
        return lockers.values
            .filter { it.hasFreeSlot(size) }
            .sortedBy { it.location.distanceKm(from) }
            .take(limit)
    }

    fun createShipment(pkg: Package, lockerId: String): Shipment {
        val locker = requireNotNull(lockers[lockerId]) { "Locker $lockerId not found" }
        require(locker.supports(pkg.size)) { "Locker $lockerId cannot hold ${pkg.size} packages" }

        val shipment = Shipment(Uuid.random(), pkg, locker)
        shipments[shipment.id] = shipment
        return shipment
    }

    fun deliver(shipmentId: Uuid): Placement? {
        val shipment = find(shipmentId)
        check(shipment.status == ShipmentStatus.PENDING) { "Shipment $shipmentId is ${shipment.status}, expected PENDING" }
        val pkg = shipment.pkg

        for (locker in nearbyLockers(shipment.preferredLocker.location, pkg.size)) {
            val slot = locker.assign(pkg) ?: continue
            val code = issuePickupCode(locker, shipment)
            val placement = Placement(locker, slot, code, clock.now() + pickupWindow)

            runCatching { shipment.markDelivered(placement) }
                .onFailure {
                    byCode.remove(codeKey(locker.id, code))
                    slot.release()
                }
                .getOrThrow()

            val where = if (locker == shipment.preferredLocker) locker.address else "${locker.address} (your chosen locker was full)"
            notifications.notify(pkg.customer, "Order ${pkg.orderId} is at $where, slot ${slot.id}. Pickup code $code, valid till ${placement.expiresAt}")
            return placement
        }

        notifications.notify(pkg.customer, "Order ${pkg.orderId} could not be delivered, no locker space nearby. We will retry")
        return null
    }

    fun pickup(lockerId: String, code: String): Shipment {
        val shipment = requireNotNull(byCode[codeKey(lockerId, code)]) { "Invalid pickup code for locker $lockerId" }
        shipment.markPickedUp(clock.now())
        release(shipment)
        return shipment
    }

    fun expireOverdue(): List<Shipment> {
        val now = clock.now()
        return shipments.values
            .filter { it.markExpired(now) }
            .onEach {
                release(it)
                notifications.notify(it.pkg.customer, "Order ${it.pkg.orderId} was not picked up and has been returned")
            }
    }

    private fun find(shipmentId: Uuid): Shipment {
        return requireNotNull(shipments[shipmentId]) { "Shipment $shipmentId not found" }
    }

    private fun issuePickupCode(locker: Locker, shipment: Shipment): String {
        while (true) {
            val code = Random.nextInt(100_000, 1_000_000).toString()
            if (byCode.putIfAbsent(codeKey(locker.id, code), shipment) == null) {
                return code
            }
        }
    }

    private fun release(shipment: Shipment) {
        val placement = shipment.placement!!
        placement.slot.release()
        byCode.remove(codeKey(placement.locker.id, placement.pickupCode))
    }

    private fun codeKey(lockerId: String, code: String): String {
        return "$lockerId:$code"
    }
}
