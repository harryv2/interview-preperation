package lld.parkingbooking.strategy

import lld.parkingbooking.entity.Slot
import lld.parkingbooking.entity.SlotType
import lld.parkingbooking.entity.TimeInterval

class FirstAvailableStrategy : SlotSelectionStrategy {
    override fun candidates(slots: List<Slot>, type: SlotType, interval: TimeInterval): List<Slot> {
        return slots.filter { it.type == type && it.isAvailable(interval) }
    }
}
