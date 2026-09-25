package lld.zomatooms.strategy

import lld.zomatooms.entity.DeliveryPartner


interface PartnerAssignmentStrategy {
    fun pick(partners: List<DeliveryPartner>): DeliveryPartner?
}


class FirstFreePartner : PartnerAssignmentStrategy {
    override fun pick(partners: List<DeliveryPartner>): DeliveryPartner? {
        return partners.firstOrNull { it.isFree() }
    }
}
