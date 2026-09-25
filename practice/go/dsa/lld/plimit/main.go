package main

import (
	"context"
	"errors"
	"fmt"
	"sync/atomic"
	"time"

	"github.com/harryv2/interview-preperation/practice/go/dsa/lld/plimit/limiter"
)

func main() {
	ctx := context.Background()

	banner("at most 3 at a time, watch the peak")
	pool := limiter.New(3)
	var running, peak atomic.Int64

	for i := 1; i <= 9; i++ {
		_, _ = limiter.Submit(pool, ctx, func(ctx context.Context) (string, error) {
			now := running.Add(1)
			for {
				high := peak.Load()
				if now <= high || peak.CompareAndSwap(high, now) {
					break
				}
			}
			time.Sleep(20 * time.Millisecond)
			running.Add(-1)
			return fmt.Sprintf("task-%d", i), nil
		})
	}
	pool.Wait()
	fmt.Printf("  9 tasks done, highest seen running at once: %d\n", peak.Load())

	banner("results come back typed")
	sizes := limiter.New(2)
	handles := make([]*limiter.Result[int], 0, 4)
	for _, word := range []string{"go", "limiter", "concurrency", "p-limit"} {
		h, _ := limiter.Submit(sizes, ctx, func(ctx context.Context) (int, error) {
			return len(word), nil
		})
		handles = append(handles, h)
	}
	for _, h := range handles {
		n, _ := h.Wait(ctx)
		fmt.Printf("  %d ", n)
	}
	fmt.Println()

	banner("a panicking task does not take the pool down")
	safe := limiter.New(2)
	bad, _ := safe.Go(ctx, func(ctx context.Context) error {
		panic("bad input")
	})
	good, _ := safe.Go(ctx, func(ctx context.Context) error {
		return nil
	})
	err := bad.Wait(ctx)
	var panicErr *limiter.PanicError
	fmt.Printf("  bad task  -> %v (is PanicError: %t)\n", err, errors.As(err, &panicErr))
	fmt.Printf("  good task -> %v, pool still alive\n", good.Wait(ctx))

	banner("a task cancelled while queued never runs")
	slow := limiter.New(1)
	_, _ = slow.Go(ctx, func(ctx context.Context) error {
		time.Sleep(50 * time.Millisecond)
		return nil
	})
	doomedCtx, cancel := context.WithCancel(ctx)
	ran := false
	doomed, _ := slow.Go(doomedCtx, func(ctx context.Context) error {
		ran = true
		return nil
	})
	cancel()
	fmt.Printf("  queued task -> %v, body executed: %t\n", doomed.Wait(ctx), ran)
	slow.Wait()

	banner("resize while work is in flight")
	elastic := limiter.New(1)
	for i := 0; i < 12; i++ {
		_, _ = elastic.Go(ctx, func(ctx context.Context) error {
			time.Sleep(15 * time.Millisecond)
			return nil
		})
	}
	fmt.Printf("  at limit 1: %d active, %d pending\n", elastic.ActiveCount(), elastic.PendingCount())
	elastic.SetConcurrency(6)
	time.Sleep(2 * time.Millisecond)
	fmt.Printf("  after raising to 6: %d active, %d pending\n", elastic.ActiveCount(), elastic.PendingCount())
	elastic.Wait()

	banner("backpressure instead of an unbounded backlog")
	tight := limiter.New(1, limiter.WithMaxQueue(2))
	accepted, rejected := 0, 0
	for i := 0; i < 8; i++ {
		_, err := tight.Go(ctx, func(ctx context.Context) error {
			time.Sleep(10 * time.Millisecond)
			return nil
		})
		if errors.Is(err, limiter.ErrQueueFull) {
			rejected++
		} else {
			accepted++
		}
	}
	fmt.Printf("  accepted %d, rejected %d with ErrQueueFull\n", accepted, rejected)
	tight.Wait()

	banner("Map keeps input order with bounded concurrency")
	urls := []string{"a", "bb", "ccc", "dddd", "eeeee"}
	lengths, err := limiter.Map(ctx, 2, urls, func(ctx context.Context, s string) (int, error) {
		time.Sleep(10 * time.Millisecond)
		return len(s), nil
	})
	fmt.Printf("  %v err=%v\n", lengths, err)

	banner("the first error cancels the rest")
	_, err = limiter.Map(ctx, 2, []int{1, 2, 3, 4, 5, 6}, func(ctx context.Context, n int) (int, error) {
		if n == 3 {
			return 0, errors.New("item 3 is bad")
		}
		select {
		case <-ctx.Done():
			return 0, ctx.Err()
		case <-time.After(20 * time.Millisecond):
			return n * n, nil
		}
	})
	fmt.Printf("  %v\n", err)
}

func banner(title string) {
	fmt.Printf("\n== %s ==\n", title)
}
