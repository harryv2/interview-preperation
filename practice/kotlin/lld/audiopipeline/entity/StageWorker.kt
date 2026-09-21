package lld.audiopipeline.entity

import lld.audiopipeline.strategies.AudioStage
import lld.audiopipeline.strategies.OverflowPolicy
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class StageWorker(
    private val stage: AudioStage,
    private val input: BlockingQueue<AudioFrame>,
    private val output: BlockingQueue<AudioFrame>,
    private val pool: BufferPool,
    private val overflow: OverflowPolicy,
    val stats: StageStats,
) {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        running = true
        thread = thread(name = "stage-${stats.name}") { run() }
    }

    fun stop() {
        running = false
        thread?.join()
    }

    private fun run() {
        while (running) {
            val frame = input.poll(5, TimeUnit.MILLISECONDS) ?: continue
            stage.process(frame)
            stats.processed.incrementAndGet()

            overflow.enqueue(output, frame) { dropped ->
                stats.dropped.incrementAndGet()
                pool.release(dropped)
            }
        }
    }
}
