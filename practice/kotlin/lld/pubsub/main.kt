package lld.pubsub

fun main() {
    val broker = Broker(Config(maxAttempts = 2, retryDelayMs = 10))

    broker.subscribe("orders", "inventory") { println("inventory: ${it.payload}") }
    broker.subscribe("orders", "email") { println("email: ${it.payload}") }
    broker.subscribe("orders", "flaky") { error("cannot process ${it.payload}") }

    broker.publish("orders", "order-123 placed")

    broker.subscribeFromStart("orders", "audit") { println("audit: ${it.payload}") }

    broker.close()
    broker.deadLetters("orders", "flaky").forEach { println("dead letter: ${it.message.payload} -> ${it.error}") }
}
