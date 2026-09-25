package lld.media.audiopipeline.strategies

import lld.media.audiopipeline.entity.AudioFrame
import kotlin.math.abs

class NoiseGateStage(private val threshold: Float) : AudioStage {
    override fun process(frame: AudioFrame) {
        for (i in frame.samples.indices) {
            if (abs(frame.samples[i]) < threshold) {
                frame.samples[i] = 0f
            }
        }
    }
}
