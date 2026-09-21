package lld.amazonlocker.strategy

import lld.amazonlocker.entity.LockerSlot
import lld.amazonlocker.entity.Size

fun interface SlotSelectionStrategy {
    fun candidates(slots: List<LockerSlot>, size: Size): List<LockerSlot>
}

class SmallestFitStrategy : SlotSelectionStrategy {
    override fun candidates(slots: List<LockerSlot>, size: Size): List<LockerSlot> {
        return slots
            .filter { it.isFree && it.size.fits(size) }
            .sortedBy { it.size }
    }
}
