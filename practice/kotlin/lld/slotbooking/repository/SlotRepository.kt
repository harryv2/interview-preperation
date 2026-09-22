package lld.slotbooking.repository

import lld.slotbooking.entity.Slot
import lld.slotbooking.entity.SlotId
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

interface SlotRepository {
    fun find(id: SlotId): Slot?
    fun findByZone(zone: String, from: LocalDateTime, to: LocalDateTime): List<Slot>
    fun tryReserve(id: SlotId): Boolean
    fun release(id: SlotId)
}

class InMemorySlotRepository : SlotRepository {
    private val store = ConcurrentHashMap<SlotId, Slot>()

    fun add(slot: Slot) {
        store[slot.id] = slot
    }

    override fun find(id: SlotId): Slot? {
        return store[id]
    }

    override fun findByZone(zone: String, from: LocalDateTime, to: LocalDateTime): List<Slot> {
        return store.values.filter { it.zone == zone && it.start >= from && it.start < to }
    }

    override fun tryReserve(id: SlotId): Boolean {
        var reserved = false
        store.computeIfPresent(id) { _, slot ->
            if (slot.booked < slot.capacity) {
                reserved = true
                slot.copy(booked = slot.booked + 1)
            } else {
                slot
            }
        }
        return reserved
    }

    override fun release(id: SlotId) {
        store.computeIfPresent(id) { _, slot ->
            slot.copy(booked = (slot.booked - 1).coerceAtLeast(0))
        }
    }
}
