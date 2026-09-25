package lld.media.audiopipeline.strategies

import lld.media.audiopipeline.entity.AudioFrame

class GainStage(private val gain: Float) : AudioStage {
    override fun process(frame: AudioFrame) {
        for (i in frame.samples.indices) {
            frame.samples[i] *= gain
        }
    }
}
