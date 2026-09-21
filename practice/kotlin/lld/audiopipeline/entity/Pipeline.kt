package lld.audiopipeline.entity

import lld.audiopipeline.strategies.AudioStage
import lld.audiopipeline.strategies.OverflowPolicy
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class Pipeline(
    private val fps: Int,
    samplesPerFrame: Int,
    stages: List<Pair<String, AudioStage>>,
    private val sink: (AudioFrame) -> Unit,
    private val overflow: OverflowPolicy,
    queueCapacity: Int = 4,
    poolSize: Int = 16,
) {
    val stats = PipelineStats()
    private val pool = BufferPool(poolSize, samplesPerFrame)
    private val queues = List(stages.size + 1) { ArrayBlockingQueue<AudioFrame>(queueCapacity) }
    private val workers: List<StageWorker>

    @Volatile private var running = false
    private var sourceThread: Thread? = null
    private var sinkThread: Thread? = null

    init {
        workers = stages.mapIndexed { i, (name, stage) ->
            val stageStats = StageStats(name)
            stats.stages += stageStats
            StageWorker(stage, queues[i], queues[i + 1], pool, overflow, stageStats)
        }
    }

    fun start() {
        running = true
        workers.forEach { it.start() }
        sinkThread = thread(name = "sink") { runSink() }
        sourceThread = thread(name = "source") { runSource() }
    }

    fun stop() {
        running = false
        sourceThread?.join()
        workers.forEach { it.stop() }
        sinkThread?.join()
    }

    // the only thread that allocates nothing and must never block: it drops instead
    private fun runSource() {
        val pacer = Pacer(fps)
        var sequence = 0L
        val fillStage = queues.first()

        while (running) {
            pacer.awaitNextTick()
            val frame = pool.acquire()
            if (frame == null) {
                stats.droppedNoBuffer.incrementAndGet()
                continue
            }

            frame.sequence = sequence++
            frame.capturedAtNanos = System.nanoTime()
            synthesize(frame)
            stats.produced.incrementAndGet()

            overflow.enqueue(fillStage, frame) { dropped ->
                stats.droppedQueueFull.incrementAndGet()
                pool.release(dropped)
            }
        }
    }

    private fun runSink() {
        val last = queues.last()
        while (running || last.isNotEmpty()) {
            val frame = last.poll(5, TimeUnit.MILLISECONDS) ?: continue
            sink(frame)
            stats.consumed.incrementAndGet()
            pool.release(frame)
        }
    }

    private fun synthesize(frame: AudioFrame) {
        for (i in frame.samples.indices) {
            frame.samples[i] = Math.sin((frame.sequence * frame.samples.size + i) * 0.05).toFloat()
        }
    }
}
