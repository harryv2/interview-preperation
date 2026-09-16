package lld_self.parkinglot.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Floor(
    var number: String,
    var slots: Array<Slot>
) {

    private var freeSlots = hashMapOf<SlotType, ArrayDeque<Slot>>()

    init {
        slots.forEach {

            require(it.floorNumber == number) {"Floor number mismatch"}

            freeSlots.putIfAbsent(it.type, ArrayDeque())
            freeSlots[it.type]?.addLast(it)
        }
    }

    var lock = ReentrantLock()

    fun getFreeSlotsCount(slotType: SlotType): Int {
        lock.withLock {
            return freeSlots[slotType]!!.size
        }
    }

    fun reserve(slotType: SlotType, vehicle: Vehicle): Slot? {
        lock.withLock {
            if(!freeSlots.contains(slotType) || freeSlots[slotType]?.size == 0) {
                return null
            }

            var slot = freeSlots[slotType]!!.removeFirst()

            slot.take(vehicle = vehicle)

            return slot
        }


    }

    fun markFree(slot: Slot) {
        lock.withLock {
            slot.free()
            freeSlots[slot.type]!!.addLast(slot)
        }
    }
}