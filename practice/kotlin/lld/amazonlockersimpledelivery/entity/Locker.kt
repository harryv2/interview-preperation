package lld.amazonlockersimpledelivery.entity

import lld.amazonlockersimpledelivery.strategy.SlotSelectionStrategy
import lld.amazonlockersimpledelivery.strategy.SmallestFitStrategy

class Locker(
    val id: String,
    val address: String,
    val location: Location,
    val slots: List<LockerSlot>,
    private val slotSelection: SlotSelectionStrategy = SmallestFitStrategy(),
) {
    fun supports(size: Size): Boolean {
        return slots.any { it.size.fits(size) }
    }

    fun hasFreeSlot(size: Size): Boolean {
        return slots.any { it.isFree && it.size.fits(size) }
    }

    fun assign(pkg: Package): LockerSlot? {
        for (slot in slotSelection.candidates(slots, pkg.size)) {
            if (slot.tryOccupy(pkg.id)) return slot
        }
        return null
    }
}
