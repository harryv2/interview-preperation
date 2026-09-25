package lld.media.audiopipeline.entity

import java.util.concurrent.ArrayBlockingQueue

class BufferPool(size: Int, samplesPerFrame: Int) {
    private val free = ArrayBlockingQueue<AudioFrame>(size)

    init {
        for (i in 0 until size) {
            free.add(AudioFrame(i, FloatArray(samplesPerFrame)))
        }
    }

    val available: Int
        get() = free.size

    fun acquire(): AudioFrame? {
        return free.poll()
    }

    fun release(frame: AudioFrame) {
        free.offer(frame)
    }
}
