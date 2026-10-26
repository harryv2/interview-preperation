package lld.rippling.cronscheduler

import java.util.concurrent.atomic.AtomicInteger

fun main() {
    CronScheduler(workers = 8).use { cron ->
        println("-- fast job every 50ms, slow job takes 120ms but fires every 50ms")
        val fast = cron.schedule("fast", 50) {}

        val skip = cron.schedule("slow-skip", 50, OverlapPolicy.SKIP) {
            Thread.sleep(120)
        }

        val queued = cron.schedule("slow-queue", 50, OverlapPolicy.QUEUE_ONE) {
            Thread.sleep(120)
        }

        val inFlight = AtomicInteger()
        val peak = AtomicInteger()
        val parallel = cron.schedule("slow-parallel", 50, OverlapPolicy.ALLOW_CONCURRENT) {
            peak.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            Thread.sleep(120)
            inFlight.decrementAndGet()
        }

        val flaky = cron.schedule("flaky", 100) {
            error("boom")
        }

        Thread.sleep(620)
        cron.cancel("fast")
        val fastRunsAtCancel = fast.runs.get()
        Thread.sleep(200)

        println("  fast:          runs=${fast.runs.get()} (no new runs after cancel: ${fast.runs.get() == fastRunsAtCancel})")
        println("  slow-skip:     runs=${skip.runs.get()} skipped=${skip.skipped.get()}")
        println("  slow-queue:    runs=${queued.runs.get()} skipped=${queued.skipped.get()}")
        println("  slow-parallel: runs=${parallel.runs.get()} peak in flight=${peak.get()}")
        println("  flaky:         failures=${flaky.failures.get()} (scheduler kept going)")
    }
}
