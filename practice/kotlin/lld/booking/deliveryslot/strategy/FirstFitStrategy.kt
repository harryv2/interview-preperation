package lld.booking.deliveryslot.strategy

import lld.booking.deliveryslot.entity.DeliveryVan

class FirstFitStrategy : VanAssignmentStrategy {
    override fun pick(vans: List<DeliveryVan>, slotId: String): DeliveryVan? {
        return vans.firstOrNull { it.hasRoomOn(slotId) }
    }
}
