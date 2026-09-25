package lld.booking.deliveryslot.strategy

import lld.booking.deliveryslot.entity.DeliveryVan

interface VanAssignmentStrategy {
    fun pick(vans: List<DeliveryVan>, slotId: String): DeliveryVan?
}
