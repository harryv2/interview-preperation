// Package limiter bounds how many tasks run at once, in the spirit of JavaScript's p-limit.
//
// A buffered channel already gives you a semaphore in five lines. This package exists for what
// that does not give you: strict FIFO admission, a result per task, a concurrency level you can
// change while work is in flight, panic isolation, and counts you can put on a dashboard.
package limiter

import (
	"context"
	"errors"
	"fmt"
	"runtime/debug"
	"sync"
)

var (
	// ErrClosed is returned when work is submitted to a closed limiter.
	ErrClosed = errors.New("limiter closed")
	// ErrQueueFull is returned when a bounded queue has no room. It is backpressure, not failure.
	ErrQueueFull = errors.New("limiter queue full")
	// ErrDropped settles the future of a task that was discarded by ClearQueue before it ran.
	ErrDropped = errors.New("task dropped from queue")
)

// PanicError carries whatever a task panicked with. One bad task must not take the pool down,
// so the panic is caught on that task's goroutine and handed back as an ordinary error.
type PanicError struct {
	Value any
	Stack []byte
}

func (e *PanicError) Error() string {
	return fmt.Sprintf("task panicked: %v", e.Value)
}

// Task is the unit of work. The context is the one supplied at submission.
type Task func(ctx context.Context) error

type job struct {
	ctx    context.Context
	task   Task
	finish func(error)
}

// Limiter admits at most Concurrency tasks at a time and queues the rest in submission order.
//
// It is deliberately not generic. Go has no generic methods, so making the type generic would pin
// one limiter to one result type. Submit is a free function instead, and every call site picks its own.
type Limiter struct {
	mu       sync.Mutex
	idle     *sync.Cond
	slots    int
	active   int
	queue    []*job
	maxQueue int
	closed   bool
}

// Option configures a Limiter at construction.
type Option func(*Limiter)

// WithMaxQueue bounds the wait queue. Submitting beyond it returns ErrQueueFull rather than
// letting an unbounded backlog eat memory that nobody is watching.
func WithMaxQueue(n int) Option {
	return func(l *Limiter) {
		l.maxQueue = n
	}
}

// New builds a limiter admitting concurrency tasks at a time.
func New(concurrency int, opts ...Option) *Limiter {
	if concurrency < 1 {
		panic("limiter: concurrency must be at least 1")
	}

	l := &Limiter{slots: concurrency}
	l.idle = sync.NewCond(&l.mu)

	for _, opt := range opts {
		opt(l)
	}
	return l
}

// Go queues a task and returns a future that settles when it finishes, is dropped, or its
// context dies. The future always settles exactly once.
func (l *Limiter) Go(ctx context.Context, task Task) (*Future, error) {
	if ctx == nil {
		ctx = context.Background()
	}

	future := newFuture()
	queued := &job{ctx: ctx, task: task, finish: future.settle}

	l.mu.Lock()
	defer l.mu.Unlock()

	if l.closed {
		return nil, ErrClosed
	}
	if l.maxQueue > 0 && len(l.queue) >= l.maxQueue {
		return nil, ErrQueueFull
	}

	l.queue = append(l.queue, queued)
	l.dispatch()
	return future, nil
}

// dispatch starts whatever the free slots allow. The caller must hold mu.
//
// An explicit queue rather than a buffered channel is what makes admission FIFO. Goroutines parked
// on a channel are woken in order today, but that is runtime behaviour and not a promise.
func (l *Limiter) dispatch() {
	for l.active < l.slots && len(l.queue) > 0 {
		next := l.queue[0]
		l.queue[0] = nil
		l.queue = l.queue[1:]
		l.active++
		go l.run(next)
	}
}

func (l *Limiter) run(j *job) {
	defer func() {
		l.mu.Lock()
		l.active--
		l.dispatch()
		if l.active == 0 && len(l.queue) == 0 {
			l.idle.Broadcast()
		}
		l.mu.Unlock()
	}()

	// a task whose context died while it sat in the queue never runs at all
	if err := j.ctx.Err(); err != nil {
		j.finish(err)
		return
	}

	j.finish(l.invoke(j))
}

func (l *Limiter) invoke(j *job) (err error) {
	defer func() {
		if recovered := recover(); recovered != nil {
			err = &PanicError{Value: recovered, Stack: debug.Stack()}
		}
	}()

	return j.task(j.ctx)
}

// Wait blocks until nothing is running and nothing is queued.
func (l *Limiter) Wait() {
	l.mu.Lock()
	defer l.mu.Unlock()

	for l.active > 0 || len(l.queue) > 0 {
		l.idle.Wait()
	}
}

// Close stops new submissions. Work already queued still runs.
func (l *Limiter) Close() {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.closed = true
}

// CloseAndWait stops new submissions and blocks until the backlog has drained.
func (l *Limiter) CloseAndWait() {
	l.Close()
	l.Wait()
}

// ClearQueue discards everything still waiting and returns how many went.
//
// Running tasks are untouched: Go cannot preempt a goroutine, it can only ask one to stop through
// its context. Cancelling work already in flight is the caller's job, through the context they passed.
func (l *Limiter) ClearQueue() int {
	l.mu.Lock()
	dropped := l.queue
	l.queue = nil
	if l.active == 0 {
		l.idle.Broadcast()
	}
	l.mu.Unlock()

	for _, j := range dropped {
		j.finish(ErrDropped)
	}
	return len(dropped)
}

// SetConcurrency changes the limit while work is in flight. Raising it starts queued tasks at once.
// Lowering it interrupts nothing, the excess drains as running tasks finish.
func (l *Limiter) SetConcurrency(n int) {
	if n < 1 {
		panic("limiter: concurrency must be at least 1")
	}

	l.mu.Lock()
	defer l.mu.Unlock()
	l.slots = n
	l.dispatch()
}

// ActiveCount is how many tasks are running right now.
func (l *Limiter) ActiveCount() int {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.active
}

// PendingCount is how many tasks are waiting for a slot.
func (l *Limiter) PendingCount() int {
	l.mu.Lock()
	defer l.mu.Unlock()
	return len(l.queue)
}

// Concurrency is the current limit.
func (l *Limiter) Concurrency() int {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.slots
}
