package lld.pubsub

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Broker(private val cfg: Config = Config()) {
    private val lock = ReentrantLock()
    private val topics = HashMap<String, Topic>()
    private var closed = false

    private fun topic(name: String): Topic {
        lock.withLock {
            check(!closed) { "broker is closed" }
            return topics.getOrPut(name) { Topic(name, cfg) }
        }
    }

    private fun lookup(name: String): Topic? {
        return lock.withLock { topics[name] }
    }

    fun publish(topicName: String, payload: String) {
        topic(topicName).publish(Message(payload = payload))
    }

    fun subscribe(topicName: String, id: String, handler: Handler): Boolean {
        return topic(topicName).subscribe(id, handler, fromStart = false)
    }

    fun subscribeFromStart(topicName: String, id: String, handler: Handler): Boolean {
        return topic(topicName).subscribe(id, handler, fromStart = true)
    }

    fun unsubscribe(topicName: String, id: String) {
        lookup(topicName)?.unsubscribe(id)
    }

    fun deadLetters(topicName: String, id: String): List<DeadLetter> {
        return lookup(topicName)?.deadLetters(id) ?: emptyList()
    }

    fun close() {
        val all = lock.withLock {
            closed = true
            topics.values.toList()
        }
        all.forEach { it.close() }
    }
}
