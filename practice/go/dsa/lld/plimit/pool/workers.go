package pool

import (
	"context"
	"sync"
)

// Map runs fn over items with at most workers at a time and returns results in INPUT ORDER.
//
// Writing results[i] from different goroutines needs no lock. Each index is written by exactly one
// goroutine, so there is no shared location to race on — only the slice header is shared and that
// is never written.
func Map[T, R any](ctx context.Context, items []T, workers int, fn func(context.Context, T) (R, error)) ([]R, error) {
	results := make([]R, len(items))

	group, ctx := NewGroup(ctx, workers)
	for i, item := range items {
		// Go 1.22 made loop variables per iteration. Before that this needed i, item := i, item
		// or every goroutine would close over the last element.
		if err := group.Go(ctx, func() error {
			value, err := fn(ctx, item)
			if err != nil {
				return err
			}
			results[i] = value
			return nil
		}); err != nil {
			break
		}
	}

	if err := group.Wait(); err != nil {
		return nil, err
	}
	return results, nil
}

// RunWorkers is the other classic shape: a fixed set of goroutines ranging over one channel.
//
// Use it when the work is a known list. Use Pool when tasks appear as you go. Same cap either way,
// the difference is who owns the goroutines — here they are started once and reused.
func RunWorkers[T any](items []T, workers int, fn func(T)) {
	if workers < 1 {
		panic("pool: workers must be at least 1")
	}

	jobs := make(chan T)
	var wg sync.WaitGroup

	for range workers {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for item := range jobs {
				fn(item)
			}
		}()
	}

	for _, item := range items {
		jobs <- item
	}

	// closing is what ends the range loops, and it must happen before Wait or this deadlocks
	close(jobs)
	wg.Wait()
}
