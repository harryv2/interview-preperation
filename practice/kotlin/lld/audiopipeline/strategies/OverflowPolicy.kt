package lld.audiopipeline.strategies

import lld.audiopipeline.entity.AudioFrame
import java.util.concurrent.BlockingQueue

// what to do when the next queue is full; every frame that does not go through is handed to onDrop
enum class OverflowPolicy {
    DROP_NEWEST {
        override fun enqueue(queue: BlockingQueue<AudioFrame>, frame: AudioFrame, onDrop: (AudioFrame) -> Unit) {
            if (!queue.offer(frame)) {
                onDrop(frame)
            }
        }
    },
    DROP_OLDEST {
        override fun enqueue(queue: BlockingQueue<AudioFrame>, frame: AudioFrame, onDrop: (AudioFrame) -> Unit) {
            while (!queue.offer(frame)) {
                val evicted = queue.poll()
                if (evicted != null) {
                    onDrop(evicted)
                }
            }
        }
    };

    abstract fun enqueue(queue: BlockingQueue<AudioFrame>, frame: AudioFrame, onDrop: (AudioFrame) -> Unit)
}
