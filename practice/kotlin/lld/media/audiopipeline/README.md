# High-Frequency Audio Pipeline

Frames flow source → stage → stage → sink at a fixed X fps. Every stage runs on its own thread; bounded queues sit between them.

## Classes

- `AudioFrame` — `FloatArray` of samples plus sequence and capture time. Mutable, reused.
- `BufferPool` — pre-allocated frames. `acquire` is non-blocking (`null` when empty); the sink `release`s frames back. No allocation on the hot path, no GC pauses.
- `Pacer` — ticks at exactly X fps using absolute deadlines (`start + n × period`), so sleep jitter does not drift.
- `StageWorker` — one thread: `poll` input, `process` in place, `enqueue` to output.
- `Pipeline` — wires source thread → `ArrayBlockingQueue`s → workers → sink thread. `start` / `stop` / `stats`.
- `PipelineStats` — atomic counters: produced, consumed, dropped (no buffer), per-stage processed / dropped.
- `strategies/AudioStage` — `process(frame)` in place: `GainStage`, `NoiseGateStage`.
- `strategies/OverflowPolicy` — full output queue: `DROP_NEWEST` (don't enqueue) or `DROP_OLDEST` (evict the head).

## Concurrency controls

- Bounded `ArrayBlockingQueue` between stages: back-pressure without blocking the producer.
- The source never blocks. If the pool or the first queue is full it drops and counts. A blocked audio source is a glitch; a dropped frame is a smaller one.
- Frames move by reference; exactly one thread owns a frame at a time (queue hand-off gives the happens-before edge). No locks on the data.
- `@Volatile running` + `poll(timeout)` for clean stop; counters are `AtomicLong`.

## Say out loud

- Frame budget = 1000 / fps ms. A stage slower than that will drop; the demo shows it.
- `DROP_OLDEST` bounds latency (queue depth × period); `DROP_NEWEST` keeps older audio intact but latency grows to the queue size.
- Java can't do hard real time; `parkNanos` jitter is ~50–100 µs. Real audio engines use a callback from the audio driver as the clock and keep the same pool + lock-free queue shape.
