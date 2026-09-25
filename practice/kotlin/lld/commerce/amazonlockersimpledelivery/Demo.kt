package lld.commerce.amazonlockersimpledelivery

import lld.commerce.amazonlockersimpledelivery.entity.Customer
import lld.commerce.amazonlockersimpledelivery.entity.Location
import lld.commerce.amazonlockersimpledelivery.entity.Locker
import lld.commerce.amazonlockersimpledelivery.entity.LockerSlot
import lld.commerce.amazonlockersimpledelivery.entity.Package
import lld.commerce.amazonlockersimpledelivery.entity.Size
import lld.commerce.amazonlockersimpledelivery.service.LockerService
import kotlin.uuid.Uuid

fun main() {
    val lockers = listOf(
        Locker("LKR-1", "MG Road", Location(12.9750, 77.6050), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM)),
        Locker("LKR-2", "Indiranagar", Location(12.9780, 77.6400), slots("S1" to Size.SMALL, "M1" to Size.MEDIUM)),
        Locker("LKR-3", "Koramangala", Location(12.9350, 77.6250), slots("S1" to Size.SMALL)),
    )
    val service = LockerService(lockers)
    val customer = Customer(Uuid.random(), "Aaryan", "+91-9999999999")

    println("-- checkout: both orders choose LKR-1, nothing is held yet")
    val first = service.createShipment(Package(Uuid.random(), "ORD-1", Size.MEDIUM, customer), "LKR-1")
    val second = service.createShipment(Package(Uuid.random(), "ORD-2", Size.MEDIUM, customer), "LKR-1")
    println("LKR-1 M1 is ${lockers[0].slots[1].status}")

    println("-- delivery: slot assigned when the agent arrives")
    val p1 = service.deliver(first.id)!!
    println("ORD-1 -> ${p1.locker.id}/${p1.slot.id}")
    val p2 = service.deliver(second.id)!!
    println("ORD-2 -> ${p2.locker.id}/${p2.slot.id}")

    println("-- no MEDIUM space anywhere for a third order")
    val third = service.createShipment(Package(Uuid.random(), "ORD-3", Size.MEDIUM, customer), "LKR-2")
    println("ORD-3 -> ${service.deliver(third.id)?.let { "${it.locker.id}/${it.slot.id}" } ?: "not delivered, still ${third.status}"}")

    println("-- pickup frees the slot, retry succeeds")
    service.pickup(p1.locker.id, p1.pickupCode)
    println("ORD-1: ${first.status}, slot ${p1.slot.status}")
    println("ORD-3 -> ${service.deliver(third.id)?.let { "${it.locker.id}/${it.slot.id}" } ?: "not delivered, still ${third.status}"}")

    runCatching { service.createShipment(Package(Uuid.random(), "ORD-4", Size.LARGE, customer), "LKR-1") }
        .onFailure { println("ORD-4: ${it.message}") }
}

private fun slots(vararg spec: Pair<String, Size>): List<LockerSlot> {
    return spec.map { (id, size) -> LockerSlot(id, size) }
}
