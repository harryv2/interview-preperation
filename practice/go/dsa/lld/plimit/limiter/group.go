package limiter

import (
	"context"
	"sync"
)

// Map applies fn to every item with at most concurrency running at once, keeping results in input
// order. The first error cancels the context handed to everything still running, the same contract
// as errgroup.WithContext, and is the error returned.
func Map[In, Out any](ctx context.Context, concurrency int, items []In, fn func(context.Context, In) (Out, error)) ([]Out, error) {
	pool := New(concurrency)
	defer pool.Close()

	ctx, cancel := context.WithCancel(ctx)
	defer cancel()

	out := make([]Out, len(items))
	futures := make([]*Future, 0, len(items))

	var once sync.Once
	var firstErr error

	for i, item := range items {
		future, err := pool.Go(ctx, func(ctx context.Context) error {
			value, err := fn(ctx, item)
			if err != nil {
				once.Do(func() {
					firstErr = err
					cancel()
				})
				return err
			}

			// writing out[i] needs no lock, every task owns exactly one slot and nobody reads
			// the slice until every future below has settled
			out[i] = value
			return nil
		})
		if err != nil {
			return nil, err
		}
		futures = append(futures, future)
	}

	for _, future := range futures {
		<-future.Done()
	}

	if firstErr != nil {
		return nil, firstErr
	}
	return out, nil
}

// ForEach is Map for work done for its side effects.
func ForEach[In any](ctx context.Context, concurrency int, items []In, fn func(context.Context, In) error) error {
	_, err := Map(ctx, concurrency, items, func(ctx context.Context, item In) (struct{}, error) {
		return struct{}{}, fn(ctx, item)
	})
	return err
}
