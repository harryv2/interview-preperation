# Concurrency-Bounded Priority Task Runner

Clients submit async tasks with a priority (HIGH, MEDIUM, LOW). Follow-up: at most `K` run at once, strict priority order, but LOW must not starve.

- `submit(name, priority, work)` returns a `CompletableFuture<T>`; exceptions complete it exceptionally
- one FIFO `ArrayDeque` per priority level, behind one lock + condition
- `K` worker threads, each loops: wait for work, `next()`, run. `K` threads = the concurrency limit, nothing else to count
- `next()`:
  1. if any queue's head has waited `>= maxWaitMillis`, take the oldest such head (aging)
  2. otherwise take the head of the highest non-empty queue
- `close()` stops accepting, lets workers drain what's queued, joins them

Why per-level FIFO queues instead of one `PriorityBlockingQueue`: priority + aging means a task's effective priority changes while it sits there, and a heap can't reorder on its own. With 3 levels, checking 3 heads is O(1) and keeps FIFO within a level.

Starvation bound: a task waits at most `maxWaitMillis` plus however long the current running tasks take.

Follow-ups:
- many priority levels (0..1000) — a `TreeMap<priority, deque>`, aging by effective priority = base − waited / step
- cancel — `future.cancel` + skip cancelled tasks in `next()`
- per tenant fairness — round robin over tenants inside each priority level
- backpressure — cap total queued tasks, `submit` blocks or rejects
- timeouts per task — run on the worker but `future.orTimeout(...)`
