package pool

import (
	"context"
	"sync"
)

// Group is Pool plus the two things an interviewer asks for next: the first error, and cancelling
// the rest of the work when it happens. It is x/sync/errgroup with a concurrency cap, written out.
type Group struct {
	sem    Semaphore
	wg     sync.WaitGroup
	cancel context.CancelFunc

	once sync.Once
	err  error
}

// NewGroup returns a group and a context that is cancelled as soon as any task fails. Pass that
// context into the tasks, which is how the survivors learn to stop.
func NewGroup(ctx context.Context, n int) (*Group, context.Context) {
	ctx, cancel := context.WithCancel(ctx)
	return &Group{sem: NewSemaphore(n), cancel: cancel}, ctx
}

// Go waits for a slot and returns ctx.Err() if the context dies while waiting, so a cancelled run
// stops queueing more work instead of admitting all of it and failing each one.
func (g *Group) Go(ctx context.Context, fn func() error) error {
	if err := g.sem.AcquireCtx(ctx); err != nil {
		return err
	}
	g.wg.Add(1)

	go func() {
		defer func() {
			g.sem.Release()
			g.wg.Done()
		}()

		if err := fn(); err != nil {
			g.fail(err)
		}
	}()
	return nil
}

// Wait blocks until every task has finished and returns the first error any of them returned.
//
// Reading g.err here needs no lock: once.Do finishes before that task's wg.Done, and wg.Wait
// returns only after every Done, so the write happens-before this read.
func (g *Group) Wait() error {
	g.wg.Wait()
	g.cancel()
	return g.err
}

func (g *Group) fail(err error) {
	g.once.Do(func() {
		g.err = err
		g.cancel()
	})
}
