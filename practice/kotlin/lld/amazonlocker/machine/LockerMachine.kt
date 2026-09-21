package lld.amazonlocker.machine

import lld.amazonlocker.entity.Locker
import lld.amazonlocker.entity.LockerSlot
import lld.amazonlocker.service.LockerService
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.uuid.Uuid

class LockerMachine(val locker: Locker, internal val backend: LockerService) {
    private val lock = ReentrantLock()

    @Volatile var state: MachineState = Idle
        private set

    fun scanPackage(packageId: Uuid) {
        transition { it.scanPackage(this, packageId) }
    }

    fun enterCode(code: String) {
        transition { it.enterCode(this, code) }
    }

    fun doorClosed() {
        transition { it.doorClosed(this) }
    }

    fun timeout() {
        transition { it.timeout(this) }
    }

    fun reset() {
        transition { it.reset(this) }
    }

    fun fault() {
        transition { OutOfService }
    }

    internal fun openDoor(slot: LockerSlot) {
        slot.doorOpen = true
        display("Door ${slot.id} opened")
    }

    internal fun closeDoor(slot: LockerSlot) {
        slot.doorOpen = false
    }

    internal fun display(message: String) {
        println("[${locker.id} screen] $message")
    }

    private fun transition(next: (MachineState) -> MachineState) {
        lock.withLock {
            val from = state
            state = next(from)
            if (state !== from) println("[${locker.id}] ${from.name} -> ${state.name}")
        }
    }
}
