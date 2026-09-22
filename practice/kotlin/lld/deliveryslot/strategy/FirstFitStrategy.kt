package lld.deliveryslot.strategy

import lld.deliveryslot.entity.DeliveryVan

class FirstFitStrategy : VanAssignmentStrategy {
    override fun pick(vans: List<DeliveryVan>, slotId: String): DeliveryVan? {
        return vans.firstOrNull { it.hasRoomOn(slotId) }
    }
}
