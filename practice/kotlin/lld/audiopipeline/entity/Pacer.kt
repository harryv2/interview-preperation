package lld.audiopipeline.entity

import java.util.concurrent.locks.LockSupport

// absolute deadlines from the start, so sleep jitter does not accumulate into drift
class Pacer(fps: Int) {
    private val periodNanos = 1_000_000_000L / fps
    private var nextTick = System.nanoTime()

    fun awaitNextTick() {
        nextTick += periodNanos
        var remaining = nextTick - System.nanoTime()
        while (remaining > 0) {
            LockSupport.parkNanos(remaining)
            remaining = nextTick - System.nanoTime()
        }
    }
}
