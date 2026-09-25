Concurrency Limited Worker (p-limit in Go)

- Run at most N tasks at once, queue the rest
- Every submission hands back a future that settles with a result or a reason there isn't one
- Admission is FIFO, the order work was submitted is the order it starts
- A panicking task must not take the pool down
- A task whose context died while it waited must never run
- The limit can change while work is in flight
- A bounded queue pushes back instead of letting a backlog eat memory

Types

Limiter        the pool, untyped, owns the slots and the queue
Future         settles once, with an error or nil
Result[T]      a Future plus a typed value
Task           func(ctx) error
PanicError     what a recovered panic is handed back as

API

    pool := limiter.New(4, limiter.WithMaxQueue(1000))

    fut, err := pool.Go(ctx, func(ctx context.Context) error { ... })
    res, err := limiter.Submit(pool, ctx, func(ctx context.Context) (int, error) { ... })

    pool.Wait()                  block until idle
    pool.Close()                 stop accepting, let the queue drain
    pool.CloseAndWait()
    pool.ClearQueue()            drop everything still waiting
    pool.SetConcurrency(8)       resize while running
    pool.ActiveCount()
    pool.PendingCount()

    out, err := limiter.Map(ctx, 4, items, fn)      bounded, ordered, first error cancels
    err := limiter.ForEach(ctx, 4, items, fn)

What you would write without this

    sem := make(chan struct{}, 4)
    var wg sync.WaitGroup

    for _, item := range items {
        wg.Add(1)
        go func() {
            defer wg.Done()
            sem <- struct{}{}
            defer func() { <-sem }()
            work(item)
        }()
    }
    wg.Wait()

Five lines, and for a lot of jobs it is the right answer. Reach for this package only when you need
something it does not give you:

- it spawns a goroutine per item up front, so a million items is a million parked goroutines
- no result per task, you close over your own variables and synchronise them yourself
- FIFO is not promised, the runtime happens to wake channel waiters in order but it is not in the spec
- no way to see how many are running or waiting
- the limit is fixed at the moment you size the channel
- a panic in any task kills the process
- no backpressure, the loop that submits never blocks and never refuses

Decisions worth defending

- The queue is an explicit slice, not a buffered channel. That is what makes admission FIFO by
  construction rather than by runtime behaviour, and it is what lets SetConcurrency start queued
  work the moment the limit goes up.
- Limiter is not generic. Go has no generic methods, so a generic type would pin one limiter to one
  result type for its whole life. Submit is a free generic function instead, and every call site
  names its own type against the same pool.
- The future always settles exactly once, on every path: task returned, task panicked, context died
  in the queue, dropped by ClearQueue. A caller blocked on Wait must never be left there.
- Panics are recovered on the task's own goroutine, because a panic crossing a goroutine boundary
  cannot be caught anywhere else. The slot is returned in a defer, so a panic cannot leak capacity.
- Lowering the limit interrupts nothing. Go cannot preempt a goroutine, it can only ask one to stop
  through its context, so the honest behaviour is to stop admitting and let the excess drain.
- Waiting and cancelling are separate. Future.Wait takes its own context, so giving up on the wait
  does not cancel the task, and cancelling the task does not require abandoning the wait.
- A bounded queue returns ErrQueueFull rather than blocking the submitter. Blocking hides the
  backlog inside the caller's goroutine, an error lets the caller shed load or slow down on purpose.
- Map writes out[i] with no lock. Each task owns exactly one slot and nothing reads the slice until
  every future has settled, so the channel closes provide the ordering.

Tests

    go test ./limiter/ -race -count=5

Covers: the limit is never exceeded and is actually reached, FIFO admission under a held slot,
typed results, panic isolation plus slot recovery, a queued task with a dead context never running,
raising and lowering the limit, Close draining while rejecting, ClearQueue settling every dropped
future, bounded queue backpressure, Map ordering and cancellation, and 50 goroutines submitting
1000 tasks concurrently.

Not modelled
- priorities, a heap instead of a FIFO queue if some work should jump the line
- per key limiting, "at most 3 in flight per customer" needs a limiter per key plus eviction
- retries and backoff, which belong in the task or a wrapper around it and not in the pool
- rate limiting, which bounds starts per second and is a different question from how many run at once
- worker reuse, this starts a goroutine per task, a long lived worker pool is the other shape and
  matters when tasks are tiny and frequent enough for goroutine setup to show up
