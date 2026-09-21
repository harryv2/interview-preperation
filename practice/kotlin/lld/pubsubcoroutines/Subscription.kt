package lld.pubsubcoroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class Subscription(
    val id: String,
    private val handler: Handler,
    private val cfg: Config,
    scope: CoroutineScope,
) {
    private val channel = Channel<Message>(cfg.buffer)
    private val dlq = CopyOnWriteArrayList<DeadLetter>()
    private val job = scope.launch {
        for (msg in channel) {
            deliver(msg)
        }
    }

    suspend fun send(message: Message) {
        channel.send(message)
    }

    private suspend fun deliver(msg: Message) {
        for (attempt in 1..cfg.maxAttempts) {
            try {
                handler(msg)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt == cfg.maxAttempts) {
                    dlq += DeadLetter(msg, e.message ?: e.toString())
                    return
                }
            }
            delay(cfg.retryDelayMs)
        }
    }

    fun deadLetters(): List<DeadLetter> {
        return dlq.toList()
    }

    suspend fun stop() {
        channel.close()
        job.join()
    }
}
