package lld.pubsub

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Topic(val name: String, private val cfg: Config) {
    private val lock = ReentrantLock()
    private val messages = ArrayList<Message>()
    private val subscriptions = HashMap<String, Subscription>()

    fun publish(message: Message) {
        lock.withLock {
            messages += message
            subscriptions.values.forEach { it.signal() }
        }
    }

    fun messageAt(offset: Int): Message? {
        return lock.withLock { messages.getOrNull(offset) }
    }

    fun subscribe(id: String, handler: Handler, fromStart: Boolean): Boolean {
        lock.withLock {
            if (id in subscriptions) return false
            val offset = if (fromStart) 0 else messages.size
            subscriptions[id] = Subscription(id, handler, this, cfg, offset)
            return true
        }
    }

    fun unsubscribe(id: String) {
        val sub = lock.withLock { subscriptions.remove(id) }
        sub?.stop()
    }

    fun deadLetters(id: String): List<DeadLetter> {
        val sub = lock.withLock { subscriptions[id] }
        return sub?.deadLetters() ?: emptyList()
    }

    fun close() {
        val subs = lock.withLock { subscriptions.values.toList() }
        subs.forEach { it.stop() }
    }
}
