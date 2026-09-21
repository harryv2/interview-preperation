package lld.pubsub

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class Subscription(
    val id: String,
    private val handler: Handler,
    private val topic: Topic,
    private val cfg: Config,
    private var offset: Int,
) {
    private val wake = ArrayBlockingQueue<Unit>(1)
    private val dlq = CopyOnWriteArrayList<DeadLetter>()
    @Volatile private var active = true
    private val worker = thread(name = "sub-$id") { run() }

    fun signal() {
        wake.offer(Unit)
    }

    private fun run() {
        while (active) {
            wake.take()
            drain()
        }
    }

    private fun drain() {
        while (true) {
            val msg = topic.messageAt(offset) ?: return
            deliver(msg)
            offset++
        }
    }

    private fun deliver(msg: Message) {
        for (attempt in 1..cfg.maxAttempts) {
            try {
                handler(msg)
                return
            } catch (e: Exception) {
                if (attempt == cfg.maxAttempts) {
                    dlq += DeadLetter(msg, e.message ?: e.toString())
                    return
                }
            }
            Thread.sleep(cfg.retryDelayMs)
        }
    }

    fun deadLetters(): List<DeadLetter> {
        return dlq.toList()
    }

    fun stop() {
        active = false
        signal()
        worker.join()
    }
}
