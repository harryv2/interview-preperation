package lld.audiopipeline

import lld.audiopipeline.entity.AudioFrame
import lld.audiopipeline.entity.Pipeline
import lld.audiopipeline.strategies.AudioStage
import lld.audiopipeline.strategies.GainStage
import lld.audiopipeline.strategies.NoiseGateStage
import lld.audiopipeline.strategies.OverflowPolicy
import java.util.concurrent.atomic.AtomicLong

fun main() {
    val fps = 200
    val samplesPerFrame = 48_000 / fps

    println("-- healthy pipeline at $fps fps for 1 second")
    run(fps, samplesPerFrame, listOf("gain" to GainStage(0.5f), "gate" to NoiseGateStage(0.05f)), OverflowPolicy.DROP_NEWEST)

    println("-- one stage too slow for the frame budget (${1000 / fps} ms), DROP_OLDEST keeps latency bounded")
    val slow = AudioStage { Thread.sleep(12) }
    run(fps, samplesPerFrame, listOf("gain" to GainStage(0.5f), "slow" to slow), OverflowPolicy.DROP_OLDEST)
}

private fun run(fps: Int, samplesPerFrame: Int, stages: List<Pair<String, AudioStage>>, overflow: OverflowPolicy) {
    val latencyNanos = AtomicLong()
    val sink = { frame: AudioFrame ->
        latencyNanos.addAndGet(System.nanoTime() - frame.capturedAtNanos)
        Unit
    }
    val pipeline = Pipeline(fps, samplesPerFrame, stages, sink, overflow)

    val started = System.nanoTime()
    pipeline.start()
    Thread.sleep(1000)
    pipeline.stop()
    val seconds = (System.nanoTime() - started) / 1e9

    val stats = pipeline.stats
    println("  produced ${stats.produced.get()} (${"%.0f".format(stats.produced.get() / seconds)} fps), consumed ${stats.consumed.get()}, source dropped: no buffer ${stats.droppedNoBuffer.get()}, queue full ${stats.droppedQueueFull.get()}")
    stats.stages.forEach { println("  $it") }
    if (stats.consumed.get() > 0) {
        println("  avg end-to-end latency ${"%.2f".format(latencyNanos.get() / stats.consumed.get() / 1e6)} ms")
    }
}
