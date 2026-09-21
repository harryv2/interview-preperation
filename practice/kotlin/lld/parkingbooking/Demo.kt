package lld.parkingbooking

import lld.parkingbooking.entity.Company
import lld.parkingbooking.entity.Slot
import lld.parkingbooking.entity.SlotType
import lld.parkingbooking.entity.TimeInterval
import lld.parkingbooking.service.ParkingLot
import lld.parkingbooking.strategy.FirstAvailableStrategy
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

fun main() {
    val slots = listOf(
        Slot("F1-C1", SlotType.CAR),
        Slot("F1-C2", SlotType.CAR),
        Slot("F1-B1", SlotType.BIKE)
    )

    val lot = ParkingLot(slots, FirstAvailableStrategy())

    val acme = Company("C1", "Acme")
    val globex = Company("C2", "Globex")

    val now = Clock.System.now()
    val morning = TimeInterval(now, now + 4.hours)
    val midday = TimeInterval(now + 2.hours, now + 6.hours)
    val evening = TimeInterval(now + 4.hours, now + 8.hours)

    val b1 = lot.book(acme, SlotType.CAR, morning)
    println("acme morning -> ${b1?.slot?.id}")

    val b2 = lot.book(globex, SlotType.CAR, midday)
    println("globex midday -> ${b2?.slot?.id}")

    val b3 = lot.book(globex, SlotType.CAR, midday)
    println("globex midday again -> ${b3?.slot?.id}")

    val b4 = lot.book(acme, SlotType.CAR, evening)
    println("acme evening -> ${b4?.slot?.id}")

    lot.cancel(b2!!.id)
    val b5 = lot.book(globex, SlotType.CAR, midday)
    println("globex midday after cancel -> ${b5?.slot?.id}")

    println("free cars for evening -> ${lot.availableSlots(SlotType.CAR, evening).map { it.id }}")

    val success = AtomicInteger(0)
    val threads = (1..10).map {
        thread {
            if (lot.book(globex, SlotType.BIKE, morning) != null) {
                success.incrementAndGet()
            }
        }
    }
    threads.forEach { it.join() }

    println("10 threads booked 1 bike slot, succeeded -> ${success.get()}")
}
