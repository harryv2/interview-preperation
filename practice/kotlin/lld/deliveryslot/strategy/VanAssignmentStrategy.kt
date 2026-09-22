package lld.deliveryslot.strategy

import lld.deliveryslot.entity.DeliveryVan

interface VanAssignmentStrategy {
    fun pick(vans: List<DeliveryVan>, slotId: String): DeliveryVan?
}
