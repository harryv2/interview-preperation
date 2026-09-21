package lld.pubsubcoroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Topic(
    val name: String,
    private val cfg: Config,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val subscriptions = HashMap<String, Subscription>()

    // the mutex is held while sending so a subscriber cannot be closed by unsubscribe mid-send
    suspend fun publish(message: Message) {
        mutex.withLock {
            subscriptions.values.forEach { it.send(message) }
        }
    }

    suspend fun subscribe(id: String, handler: Handler): Boolean {
        mutex.withLock {
            if (id in subscriptions) return false
            subscriptions[id] = Subscription(id, handler, cfg, scope)
            return true
        }
    }

    suspend fun unsubscribe(id: String) {
        val sub = mutex.withLock { subscriptions.remove(id) }
        sub?.stop()
    }

    suspend fun deadLetters(id: String): List<DeadLetter> {
        val sub = mutex.withLock { subscriptions[id] }
        return sub?.deadLetters() ?: emptyList()
    }

    suspend fun close() {
        val subs = mutex.withLock { subscriptions.values.toList() }
        subs.forEach { it.stop() }
    }
}
