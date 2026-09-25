package lld.booking.parkingbooking.strategy

import lld.booking.parkingbooking.entity.Slot
import lld.booking.parkingbooking.entity.SlotType
import lld.booking.parkingbooking.entity.TimeInterval

interface SlotSelectionStrategy {
    fun candidates(slots: List<Slot>, type: SlotType, interval: TimeInterval): List<Slot>
}
