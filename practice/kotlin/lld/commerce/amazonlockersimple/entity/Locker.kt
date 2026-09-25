package lld.commerce.amazonlockersimple.entity

import lld.commerce.amazonlockersimple.strategy.SlotSelectionStrategy
import lld.commerce.amazonlockersimple.strategy.SmallestFitStrategy

class Locker(
    val id: String,
    val address: String,
    val location: Location,
    val slots: List<LockerSlot>,
    private val slotSelection: SlotSelectionStrategy = SmallestFitStrategy(),
) {
    fun hasFreeSlot(size: Size): Boolean {
        return slots.any { it.isFree && it.size.fits(size) }
    }

    fun reserve(pkg: Package): LockerSlot? {
        for (slot in slotSelection.candidates(slots, pkg.size)) {
            if (slot.tryReserve(pkg.id)) return slot
        }
        return null
    }
}
