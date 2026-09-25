pool — the concurrency limiter you can write in an interview

../limiter is the shipped version: futures, FIFO admission, live resizing, panic isolation, bounded
queue, metrics. 228 lines before tests. This is the version you write when someone says "cap this at
N at a time" and you have a whiteboard and four minutes.

The one idea

    sem := make(chan struct{}, n)     // n permits
    sem <- struct{}{}                 // take one, blocks when they are gone
    <-sem                             // give it back

A buffered channel IS a counting semaphore. Say that sentence and most of the question is answered.

Four things, in the order an interviewer asks for them

1. Semaphore            take and give back permits, with TryAcquire and a context aware Acquire
2. Pool                 Go(fn) blocks while full, Wait() waits for everything
3. Group                Pool plus the first error, and a context that cancels the rest
4. Map / RunWorkers     the same cap applied to a list you already have

Pool is about twenty lines:

    func (p *Pool) Go(fn func()) {
        p.sem.Acquire()
        p.wg.Add(1)
        go func() {
            defer func() { p.sem.Release(); p.wg.Done() }()
            fn()
        }()
    }

Things worth saying out loud while you write it

wg.Add runs on the caller's goroutine, never inside the new one. Put it inside and Wait can return
before the task has registered itself. This is the single most common WaitGroup bug.

Go blocking when the pool is full is not a limitation, it is the backpressure. The producer cannot
outrun the workers and stack up a million goroutines behind them. If you do not want to block,
that is what TryAcquire is for.

Map writes results[i] from different goroutines with no lock, and that is correct: each index is
written by exactly one goroutine, so there is no shared location to race on. Only the slice header
is shared and nothing writes it. The tests run under -race to prove it.

Group.Wait reads g.err with no lock, and that is correct too: once.Do completes before that task's
wg.Done, and wg.Wait returns only after every Done, so there is a happens-before edge.

Go 1.22 made loop variables per iteration. Before that, every closure in a range loop captured the
same variable and you needed i, item := i, item. Knowing which side of that line you are on is worth
one sentence.

Pool versus RunWorkers

    Pool          tasks appear as you go, a goroutine per task, started and thrown away
    RunWorkers    the work is a known list, N goroutines started once and reused

Same cap either way. RunWorkers is cheaper when there are a lot of small tasks because it does not
start a goroutine per item, but goroutines are cheap enough that this rarely decides it. Pick the one
that matches the shape of the work.

What this deliberately does not do, and where ../limiter picks it up

- no FIFO promise. Goroutines parked on a channel send are woken in order today, but that is runtime
  behaviour, not a documented guarantee. If admission order is a requirement you need an explicit
  queue, which is exactly why limiter has one
- no result per task. Group gives you the first error, not one outcome per submission
- no panic isolation. A panicking task takes the process down here. limiter recovers on the task's
  own goroutine and hands the panic back as an error
- no changing the cap while work is in flight. The channel's capacity is fixed at make time
- no bounded queue and no way to drop what is still waiting

Run it

    go test -race ./pool/
