package lld.messaging.pubsubcoroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Broker(private val cfg: Config = Config()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val topics = HashMap<String, Topic>()
    private var closed = false

    private suspend fun topic(name: String): Topic {
        mutex.withLock {
            check(!closed) { "broker is closed" }
            return topics.getOrPut(name) { Topic(name, cfg, scope) }
        }
    }

    private suspend fun lookup(name: String): Topic? {
        return mutex.withLock { topics[name] }
    }

    suspend fun publish(topicName: String, payload: String) {
        topic(topicName).publish(Message(payload = payload))
    }

    suspend fun subscribe(topicName: String, id: String, handler: Handler): Boolean {
        return topic(topicName).subscribe(id, handler)
    }

    suspend fun unsubscribe(topicName: String, id: String) {
        lookup(topicName)?.unsubscribe(id)
    }

    suspend fun deadLetters(topicName: String, id: String): List<DeadLetter> {
        return lookup(topicName)?.deadLetters(id) ?: emptyList()
    }

    suspend fun close() {
        val all = mutex.withLock {
            closed = true
            topics.values.toList()
        }
        all.forEach { it.close() }
        scope.cancel()
    }
}
