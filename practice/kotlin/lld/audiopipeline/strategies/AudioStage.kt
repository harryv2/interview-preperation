package lld.audiopipeline.strategies

import lld.audiopipeline.entity.AudioFrame

// processes samples in place; the frame object is passed along by reference
fun interface AudioStage {
    fun process(frame: AudioFrame)
}
