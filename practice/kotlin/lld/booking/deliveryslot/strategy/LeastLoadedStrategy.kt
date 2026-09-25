package lld.booking.deliveryslot.strategy

import lld.booking.deliveryslot.entity.DeliveryVan

class LeastLoadedStrategy : VanAssignmentStrategy {
    override fun pick(vans: List<DeliveryVan>, slotId: String): DeliveryVan? {
        return vans
            .filter { it.hasRoomOn(slotId) }
            .minByOrNull { it.loadOn(slotId) }
    }
}
