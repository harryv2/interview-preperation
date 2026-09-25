package lld.commerce.amazonlockersimple.strategy

import lld.commerce.amazonlockersimple.entity.LockerSlot
import lld.commerce.amazonlockersimple.entity.Size

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
