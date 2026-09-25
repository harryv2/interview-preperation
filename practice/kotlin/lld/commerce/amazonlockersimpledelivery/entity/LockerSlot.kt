package lld.commerce.amazonlockersimpledelivery.entity

import java.util.concurrent.atomic.AtomicReference
import kotlin.uuid.Uuid

enum class SlotStatus {
    FREE,
    OCCUPIED,
}

class LockerSlot(val id: String, val size: Size) {
    private val state = AtomicReference(SlotStatus.FREE)

    var packageId: Uuid? = null

    val status: SlotStatus
        get() = state.get()

    val isFree: Boolean
        get() = state.get() == SlotStatus.FREE

    fun tryOccupy(packageId: Uuid): Boolean {
        if (!state.compareAndSet(SlotStatus.FREE, SlotStatus.OCCUPIED)) return false
        this.packageId = packageId
        return true
    }

    fun release() {
        packageId = null
        state.set(SlotStatus.FREE)
    }
}
