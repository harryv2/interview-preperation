package lld.media.audiopipeline.strategies

import lld.media.audiopipeline.entity.AudioFrame

// processes samples in place; the frame object is passed along by reference
fun interface AudioStage {
    fun process(frame: AudioFrame)
}
