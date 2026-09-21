package lld.amazonlocker

import lld.amazonlocker.entity.Customer
import lld.amazonlocker.entity.Location
import lld.amazonlocker.entity.Locker
import lld.amazonlocker.entity.LockerSlot
import lld.amazonlocker.entity.Package
import lld.amazonlocker.entity.Size
import lld.amazonlocker.machine.LockerMachine
import lld.amazonlocker.service.LockerService
import kotlin.uuid.Uuid

fun main() {
    val lockers = listOf(
        Locker("LKR-1", "MG Road", Location(12.9750, 77.6050), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM, "L1" to Size.LARGE)),
        Locker("LKR-2", "Indiranagar", Location(12.9780, 77.6400), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM)),
        Locker("LKR-3", "Koramangala", Location(12.9350, 77.6250), slots("L1" to Size.LARGE, "L2" to Size.LARGE)),
    )
    val service = LockerService(lockers)
    val kiosk = LockerMachine(lockers[0], service)

    val customer = Customer(Uuid.random(), "Aaryan", "+91-9999999999")
    val userLocation = Location(12.9716, 77.5946)
    val pkg = Package(Uuid.random(), "ORD-1", Size.MEDIUM, customer)

    println("nearby lockers for a ${pkg.size} package:")
    val nearby = service.nearbyLockers(userLocation, pkg.size)
    nearby.forEach { println("  ${it.id} ${it.address} %.1f km".format(it.location.distanceKm(userLocation))) }

    val reservation = requireNotNull(service.reserve(pkg, nearby.first().id)) { "no slot for ${pkg.orderId}" }
    println("${pkg.orderId}: reserved ${reservation.locker.id}/${reservation.slot.id} -> ${reservation.status}, slot ${reservation.slot.status}")

    println("-- agent at kiosk")
    kiosk.scanPackage(pkg.id)
    kiosk.doorClosed()
    println("${pkg.orderId}: ${reservation.status}, slot ${reservation.slot.status}")

    println("-- customer at kiosk")
    kiosk.enterCode("000000")
    kiosk.enterCode(reservation.pickupCode!!)
    kiosk.timeout()
    kiosk.doorClosed()
    println("${pkg.orderId}: ${reservation.status}, slot ${reservation.slot.status}")

    println("-- maintenance")
    kiosk.fault()
    kiosk.enterCode("123456")
    kiosk.reset()
}

private fun slots(vararg spec: Pair<String, Size>): List<LockerSlot> {
    return spec.map { (id, size) -> LockerSlot(id, size) }
}
