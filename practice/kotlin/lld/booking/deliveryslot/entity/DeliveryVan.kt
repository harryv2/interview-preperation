package lld.booking.deliveryslot.entity

import lld.booking.deliveryslot.exception.VanFullException

class DeliveryVan(
    val id: String,
    val numberPlate: String,
    val capacity: Int
) {
    private val load = HashMap<String, Int>()

    fun loadOn(slotId: String): Int {
        return load[slotId] ?: 0
    }

    fun hasRoomOn(slotId: String): Boolean {
        return loadOn(slotId) < capacity
    }

    fun assign(slotId: String) {
        if (!hasRoomOn(slotId)) {
            throw VanFullException("Van $id is full on slot $slotId")
        }
        load[slotId] = loadOn(slotId) + 1
    }

    fun release(slotId: String) {
        val current = loadOn(slotId)
        if (current > 0) {
            load[slotId] = current - 1
        }
    }
}
