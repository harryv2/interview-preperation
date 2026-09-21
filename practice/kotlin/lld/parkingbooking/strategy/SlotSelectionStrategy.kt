package lld.parkingbooking.strategy

import lld.parkingbooking.entity.Slot
import lld.parkingbooking.entity.SlotType
import lld.parkingbooking.entity.TimeInterval

interface SlotSelectionStrategy {
    fun candidates(slots: List<Slot>, type: SlotType, interval: TimeInterval): List<Slot>
}
