package lld.rippling.prioritytaskrunner

import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    println("-- 1 worker: strict priority order once the worker is free")
    val order = Collections.synchronizedList(ArrayList<String>())
    PriorityTaskRunner(workers = 1, maxWaitMillis = 10_000).use { runner ->
        runner.submit("blocker", Priority.LOW) { Thread.sleep(50) }
        for ((name, priority) in listOf("low1" to Priority.LOW, "med1" to Priority.MEDIUM, "high1" to Priority.HIGH, "high2" to Priority.HIGH, "med2" to Priority.MEDIUM)) {
            runner.submit(name, priority) { order += name }
        }
    }
    println("  $order")

    println("-- aging: a LOW task that waited past maxWait jumps the HIGH queue")
    val aged = Collections.synchronizedList(ArrayList<String>())
    PriorityTaskRunner(workers = 1, maxWaitMillis = 100).use { runner ->
        runner.submit("blocker", Priority.HIGH) { Thread.sleep(60) }
        runner.submit("low", Priority.LOW) { aged += "low" }
        for (i in 1..5) {
            runner.submit("high$i", Priority.HIGH) {
                aged += "high$i"
                Thread.sleep(30)
            }
        }
    }
    println("  $aged")

    println("-- at most K run at once")
    val running = AtomicInteger()
    val peak = AtomicInteger()
    PriorityTaskRunner(workers = 3, maxWaitMillis = 1000).use { runner ->
        val futures = (1..12).map { i ->
            runner.submit("job$i", Priority.entries[i % 3]) {
                val now = running.incrementAndGet()
                peak.accumulateAndGet(now, ::maxOf)
                Thread.sleep(20)
                running.decrementAndGet()
                i * i
            }
        }
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        println("  results=${futures.map { it.join() }}")
    }
    println("  peak concurrency=${peak.get()} (limit 3)")

    println("-- failures surface on the future")
    PriorityTaskRunner(workers = 1, maxWaitMillis = 1000).use { runner ->
        val failed = runner.submit("boom", Priority.HIGH) { error("boom") }
        println("  ${runCatching { failed.join() }.exceptionOrNull()?.cause?.message}")
    }
}
