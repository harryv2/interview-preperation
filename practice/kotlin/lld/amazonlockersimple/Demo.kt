package lld.amazonlockersimple

import lld.amazonlockersimple.entity.Customer
import lld.amazonlockersimple.entity.Location
import lld.amazonlockersimple.entity.Locker
import lld.amazonlockersimple.entity.LockerSlot
import lld.amazonlockersimple.entity.Package
import lld.amazonlockersimple.entity.Size
import lld.amazonlockersimple.service.LockerService
import kotlin.uuid.Uuid

fun main() {
    val lockers = listOf(
        Locker("LKR-1", "MG Road", Location(12.9750, 77.6050), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM, "L1" to Size.LARGE)),
        Locker("LKR-2", "Indiranagar", Location(12.9780, 77.6400), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM)),
        Locker("LKR-3", "Koramangala", Location(12.9350, 77.6250), slots("L1" to Size.LARGE, "L2" to Size.LARGE)),
    )
    val service = LockerService(lockers)

    val customer = Customer(Uuid.random(), "Aaryan", "+91-9999999999")
    val userLocation = Location(12.9716, 77.5946)
    val pkg = Package(Uuid.random(), "ORD-1", Size.MEDIUM, customer)

    println("nearby lockers for a ${pkg.size} package:")
    val nearby = service.nearbyLockers(userLocation, pkg.size)
    nearby.forEach { println("  ${it.id} ${it.address} %.1f km".format(it.location.distanceKm(userLocation))) }

    val reservation = requireNotNull(service.reserve(pkg, nearby.first().id)) { "no slot for ${pkg.orderId}" }
    println("${pkg.orderId}: reserved ${reservation.locker.id}/${reservation.slot.id} -> ${reservation.status}, slot ${reservation.slot.status}")

    service.deliver(reservation.id)
    println("${pkg.orderId}: agent delivered -> ${reservation.status}, slot ${reservation.slot.status}")

    service.pickup(reservation.locker.id, reservation.pickupCode!!)
    println("${pkg.orderId}: customer picked up -> ${reservation.status}, slot ${reservation.slot.status}")

    val big1 = service.reserve(Package(Uuid.random(), "ORD-2", Size.LARGE, customer), "LKR-1")
    val big2 = service.reserve(Package(Uuid.random(), "ORD-3", Size.LARGE, customer), "LKR-1")
    println("ORD-2: reserved ${big1?.slot?.id}")
    println("ORD-3: reserved ${big2?.slot?.id ?: "none, no free LARGE slot in LKR-1"}")
}

private fun slots(vararg spec: Pair<String, Size>): List<LockerSlot> {
    return spec.map { (id, size) -> LockerSlot(id, size) }
}
