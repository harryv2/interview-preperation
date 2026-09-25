// Package pool is the small version of ../limiter: what you would actually write on a whiteboard
// when someone says "cap this at N at a time".
//
// limiter is the shipped thing — futures, FIFO admission, live resizing, panic isolation. This is
// the thirty line answer that covers the question as asked.
package pool

import (
	"context"
	"sync"
)

// Semaphore is the whole idea in one type. A buffered channel of capacity n hands out n permits:
// a send takes one and blocks once they are gone, a receive puts one back.
type Semaphore chan struct{}

func NewSemaphore(n int) Semaphore {
	if n < 1 {
		panic("pool: n must be at least 1")
	}
	return make(Semaphore, n)
}

func (s Semaphore) Acquire() {
	s <- struct{}{}
}

func (s Semaphore) Release() {
	<-s
}

// TryAcquire takes a permit if one is free and says so rather than blocking.
func (s Semaphore) TryAcquire() bool {
	select {
	case s <- struct{}{}:
		return true
	default:
		return false
	}
}

// AcquireCtx waits for a permit but gives up if the context dies first.
func (s Semaphore) AcquireCtx(ctx context.Context) error {
	select {
	case s <- struct{}{}:
		return nil
	case <-ctx.Done():
		return ctx.Err()
	}
}

// Pool runs at most n functions at once.
//
// Go blocking while the pool is full IS the backpressure: the producer cannot outrun the workers
// and pile up a million goroutines behind them.
type Pool struct {
	sem Semaphore
	wg  sync.WaitGroup
}

func New(n int) *Pool {
	return &Pool{sem: NewSemaphore(n)}
}

func (p *Pool) Go(fn func()) {
	p.sem.Acquire()

	// Add runs on the CALLER's goroutine, never inside the new one. Add it inside and Wait can
	// return before the task has registered itself, which is the classic WaitGroup bug.
	p.wg.Add(1)

	go func() {
		defer func() {
			p.sem.Release()
			p.wg.Done()
		}()
		fn()
	}()
}

func (p *Pool) Wait() {
	p.wg.Wait()
}
