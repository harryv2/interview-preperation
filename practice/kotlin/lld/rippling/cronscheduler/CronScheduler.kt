package lld.rippling.cronscheduler

import java.util.PriorityQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class OverlapPolicy { SKIP, QUEUE_ONE, ALLOW_CONCURRENT }

class Job(
    val id: String,
    val intervalMillis: Long,
    val policy: OverlapPolicy,
    val action: () -> Unit,
) {
    var nextRunAt = 0L
    var cancelled = false
    var running = 0
    var pending = false
    val runs = AtomicInteger()
    val skipped = AtomicInteger()
    val failures = AtomicInteger()
}

class CronScheduler(workers: Int) : AutoCloseable {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val queue = PriorityQueue<Job>(compareBy { it.nextRunAt })
    private val jobs = HashMap<String, Job>()
    private val pool: ExecutorService = Executors.newFixedThreadPool(workers)
    private var active = true

    private val ticker = Thread({ tickLoop() }, "cron-ticker").apply {
        isDaemon = true
        start()
    }

    fun schedule(
        id: String,
        intervalMillis: Long,
        policy: OverlapPolicy = OverlapPolicy.SKIP,
        initialDelayMillis: Long = intervalMillis,
        action: () -> Unit,
    ): Job {
        require(intervalMillis > 0) { "Interval must be positive" }
        val job = Job(id, intervalMillis, policy, action)
        lock.withLock {
            require(!jobs.containsKey(id)) { "Job $id already scheduled" }
            job.nextRunAt = System.currentTimeMillis() + initialDelayMillis
            jobs[id] = job
            queue += job
            changed.signal()
        }
        return job
    }

    fun cancel(id: String) {
        lock.withLock {
            val job = jobs.remove(id) ?: return
            job.cancelled = true
            queue.remove(job)
            changed.signal()
        }
    }

    override fun close() {
        lock.withLock {
            active = false
            changed.signalAll()
        }
        ticker.join()
        pool.shutdown()
        pool.awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun tickLoop() {
        lock.withLock {
            while (active) {
                val head = queue.peek()
                if (head == null) {
                    changed.await()
                    continue
                }
                val now = System.currentTimeMillis()
                val wait = head.nextRunAt - now
                if (wait > 0) {
                    changed.await(wait, TimeUnit.MILLISECONDS)
                    continue
                }

                queue.poll()
                fire(head)
                while (head.nextRunAt <= now) {
                    head.nextRunAt += head.intervalMillis
                }
                queue += head
            }
        }
    }

    private fun fire(job: Job) {
        synchronized(job) {
            if (job.running == 0 || job.policy == OverlapPolicy.ALLOW_CONCURRENT) {
                job.running++
                pool.execute { execute(job) }
                return
            }
            if (job.policy == OverlapPolicy.QUEUE_ONE && !job.pending) {
                job.pending = true
                return
            }
            job.skipped.incrementAndGet()
        }
    }

    private fun execute(job: Job) {
        while (true) {
            try {
                job.action()
                job.runs.incrementAndGet()
            } catch (e: Exception) {
                job.failures.incrementAndGet()
            }
            synchronized(job) {
                if (job.pending && !job.cancelled) {
                    job.pending = false
                } else {
                    job.running--
                    return
                }
            }
        }
    }
}
