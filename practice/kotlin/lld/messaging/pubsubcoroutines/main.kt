package lld.messaging.pubsubcoroutines

import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val broker = Broker(Config(buffer = 64, maxAttempts = 2, retryDelayMs = 10))

        broker.subscribe("orders", "inventory") { println("inventory: ${it.payload}") }
        broker.subscribe("orders", "email") { println("email: ${it.payload}") }
        broker.subscribe("orders", "flaky") { error("cannot process ${it.payload}") }

        broker.publish("orders", "order-123 placed")

        broker.close()
        broker.deadLetters("orders", "flaky").forEach { println("dead letter: ${it.message.payload} -> ${it.error}") }
    }
}
