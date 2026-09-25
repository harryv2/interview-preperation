package lld.booking.parkingbooking.strategy

import lld.booking.parkingbooking.entity.Slot
import lld.booking.parkingbooking.entity.SlotType
import lld.booking.parkingbooking.entity.TimeInterval

class FirstAvailableStrategy : SlotSelectionStrategy {
    override fun candidates(slots: List<Slot>, type: SlotType, interval: TimeInterval): List<Slot> {
        return slots.filter { it.type == type && it.isAvailable(interval) }
    }
}
