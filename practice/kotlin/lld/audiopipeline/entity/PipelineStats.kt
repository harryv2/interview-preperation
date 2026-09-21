package lld.audiopipeline.entity

import java.util.concurrent.atomic.AtomicLong

class StageStats(val name: String) {
    val processed = AtomicLong()
    val dropped = AtomicLong()

    override fun toString(): String {
        return "$name processed=${processed.get()} dropped=${dropped.get()}"
    }
}

class PipelineStats {
    val produced = AtomicLong()
    val droppedNoBuffer = AtomicLong()
    val droppedQueueFull = AtomicLong()
    val consumed = AtomicLong()
    val stages = ArrayList<StageStats>()
}
