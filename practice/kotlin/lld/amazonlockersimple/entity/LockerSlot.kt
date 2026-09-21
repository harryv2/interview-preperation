package lld.amazonlockersimple.entity

import java.util.concurrent.atomic.AtomicReference
import kotlin.uuid.Uuid

enum class SlotStatus {
    FREE,
    RESERVED,
    OCCUPIED,
}

class LockerSlot(val id: String, val size: Size) {
    private val state = AtomicReference(SlotStatus.FREE)

    var packageId: Uuid? = null

    val status: SlotStatus
        get() = state.get()

    val isFree: Boolean
        get() = state.get() == SlotStatus.FREE

    fun tryReserve(packageId: Uuid): Boolean {
        if (!state.compareAndSet(SlotStatus.FREE, SlotStatus.RESERVED)) return false
        this.packageId = packageId
        return true
    }

    fun occupy() {
        check(state.compareAndSet(SlotStatus.RESERVED, SlotStatus.OCCUPIED)) { "Slot $id is $status, expected RESERVED" }
    }

    fun release() {
        packageId = null
        state.set(SlotStatus.FREE)
    }
}
