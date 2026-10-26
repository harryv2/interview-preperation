package lld.rippling.prioritytaskrunner

import java.util.EnumMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class Priority { HIGH, MEDIUM, LOW }

class PriorityTaskRunner(
    workers: Int,
    private val maxWaitMillis: Long,
) : AutoCloseable {

    private class Task(val name: String, val seq: Long, val submittedAt: Long, val run: () -> Unit)

    private val lock = ReentrantLock()
    private val hasWork = lock.newCondition()
    private val queues = EnumMap<Priority, ArrayDeque<Task>>(Priority::class.java)
    private var accepting = true
    private var nextSeq = 0L

    private val threads: List<Thread>

    init {
        require(workers > 0) { "Need at least one worker" }
        for (priority in Priority.entries) {
            queues[priority] = ArrayDeque()
        }
        threads = List(workers) { i ->
            Thread({ workLoop() }, "worker-$i").apply {
                isDaemon = true
                start()
            }
        }
    }

    fun <T> submit(name: String, priority: Priority, work: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        lock.withLock {
            check(accepting) { "Runner is shut down" }
            val task = Task(name, nextSeq++, System.currentTimeMillis()) {
                try {
                    future.complete(work())
                } catch (e: Throwable) {
                    future.completeExceptionally(e)
                }
            }
            queues.getValue(priority).addLast(task)
            hasWork.signal()
        }
        return future
    }

    override fun close() {
        lock.withLock {
            accepting = false
            hasWork.signalAll()
        }
        threads.forEach { it.join() }
    }

    private fun workLoop() {
        while (true) {
            val task = next() ?: return
            task.run()
        }
    }

    private fun next(): Task? {
        lock.withLock {
            while (queues.values.all { it.isEmpty() }) {
                if (!accepting) {
                    return null
                }
                hasWork.await()
            }

            val now = System.currentTimeMillis()
            val starved = queues.values
                .mapNotNull { it.firstOrNull() }
                .filter { now - it.submittedAt >= maxWaitMillis }
                .minByOrNull { it.seq }
            if (starved != null) {
                queues.values.first { it.firstOrNull() === starved }.removeFirst()
                return starved
            }

            return queues.values.first { it.isNotEmpty() }.removeFirst()
        }
    }
}
