package lld.audiopipeline.entity

// reused from the pool, never allocated on the hot path
class AudioFrame(val id: Int, val samples: FloatArray) {
    var sequence = 0L
    var capturedAtNanos = 0L
}
