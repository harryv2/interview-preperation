package lld.deliveryslot.entity

import lld.deliveryslot.exception.SlotNotAvailableException
import lld.deliveryslot.strategy.VanAssignmentStrategy

class Warehouse(
    val id: String,
    val name: String,
    val servingZips: Set<Int>,
    private val vans: List<DeliveryVan>,
    private val strategy: VanAssignmentStrategy
) {
    fun serves(zip: Int): Boolean {
        return zip in servingZips
    }

    fun hasCapacityOn(slotId: String): Boolean {
        return vans.any { it.hasRoomOn(slotId) }
    }

    fun assignVan(slotId: String): DeliveryVan {
        val van = strategy.pick(vans, slotId)
            ?: throw SlotNotAvailableException("Slot $slotId is full at warehouse $id")
        van.assign(slotId)
        return van
    }

    fun releaseVan(vanId: String, slotId: String) {
        vans.firstOrNull { it.id == vanId }?.release(slotId)
    }
}
