package lld.booking.deliveryslot

fun main() {
    val app = DeliveryApp()

    app.addWarehouse(id = "W1", vanCount = 2, vanCapacity = 2, zips = setOf(560001, 560002))
    app.addCustomer("C1", "Asha", 560001)
    app.addCustomer("C2", "Ravi", 560001)
    app.createOrder("O1", "C1")
    app.createOrder("O2", "C2")

    val slots = app.getAvailableSlots("O1")
    println("Available: ${slots.size} slots")
    slots.take(3).forEach { println("  ${it.id}  ${it.start} - ${it.end}") }

    val target = slots.first().id

    val b1 = app.bookSlot("O1", target)
    println("Booked O1 -> slot ${b1.slotId} on van ${b1.vanId}")

    val b2 = app.bookSlot("O2", target)
    println("Booked O2 -> slot ${b2.slotId} on van ${b2.vanId}")

    val b3 = app.rescheduleBooking(b2.id, slots[1].id)
    println("Rescheduled O2 -> slot ${b3.slotId} on van ${b3.vanId}")

    app.cancelBooking(b1.id)
    println("Cancelled ${b1.id}")
}
