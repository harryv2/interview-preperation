package pool

import (
	"context"
	"errors"
	"fmt"
	"sync/atomic"
	"testing"
	"time"
)

// peak tracks the highest number of tasks that were ever in flight together.
type peak struct {
	running atomic.Int64
	highest atomic.Int64
}

func (p *peak) enter() {
	now := p.running.Add(1)
	for {
		seen := p.highest.Load()
		if now <= seen || p.highest.CompareAndSwap(seen, now) {
			return
		}
	}
}

func (p *peak) leave() {
	p.running.Add(-1)
}

func TestPoolCapsConcurrency(t *testing.T) {
	var p peak
	var done atomic.Int64

	pool := New(3)
	for range 20 {
		pool.Go(func() {
			p.enter()
			time.Sleep(2 * time.Millisecond)
			done.Add(1)
			p.leave()
		})
	}
	pool.Wait()

	if got := p.highest.Load(); got > 3 {
		t.Fatalf("peak concurrency %d, want at most 3", got)
	}
	if got := done.Load(); got != 20 {
		t.Fatalf("ran %d tasks, want 20", got)
	}
}

func TestSemaphoreTryAcquire(t *testing.T) {
	s := NewSemaphore(1)

	if !s.TryAcquire() {
		t.Fatal("first TryAcquire should succeed")
	}
	if s.TryAcquire() {
		t.Fatal("second TryAcquire should fail, the permit is out")
	}

	s.Release()
	if !s.TryAcquire() {
		t.Fatal("TryAcquire should succeed once the permit is back")
	}
}

func TestSemaphoreAcquireCtxGivesUp(t *testing.T) {
	s := NewSemaphore(1)
	s.Acquire()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Millisecond)
	defer cancel()

	if err := s.AcquireCtx(ctx); !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("got %v, want DeadlineExceeded", err)
	}
}

func TestGroupReturnsFirstErrorAndCancelsTheRest(t *testing.T) {
	boom := errors.New("boom")
	group, ctx := NewGroup(context.Background(), 2)

	var cancelled atomic.Int64

	for i := range 10 {
		if err := group.Go(ctx, func() error {
			if i == 0 {
				return boom
			}
			select {
			case <-ctx.Done():
				cancelled.Add(1)
				return nil
			case <-time.After(time.Second):
				return nil
			}
		}); err != nil {
			break
		}
	}

	if err := group.Wait(); !errors.Is(err, boom) {
		t.Fatalf("got %v, want boom", err)
	}
	if cancelled.Load() == 0 {
		t.Fatal("the surviving tasks were never told to stop")
	}
}

func TestGroupSucceeds(t *testing.T) {
	group, ctx := NewGroup(context.Background(), 4)
	var total atomic.Int64

	for i := range 50 {
		if err := group.Go(ctx, func() error {
			total.Add(int64(i))
			return nil
		}); err != nil {
			t.Fatalf("Go: %v", err)
		}
	}

	if err := group.Wait(); err != nil {
		t.Fatalf("Wait: %v", err)
	}
	if got := total.Load(); got != 1225 {
		t.Fatalf("total %d, want 1225", got)
	}
}

func TestMapKeepsInputOrder(t *testing.T) {
	items := []int{5, 3, 1, 4, 2, 9, 7, 8, 6, 0}

	got, err := Map(context.Background(), items, 4, func(_ context.Context, n int) (string, error) {
		// sleep inversely to the value so completion order is nothing like input order
		time.Sleep(time.Duration(10-n) * time.Millisecond)
		return fmt.Sprintf("v%d", n), nil
	})
	if err != nil {
		t.Fatalf("Map: %v", err)
	}

	for i, n := range items {
		if want := fmt.Sprintf("v%d", n); got[i] != want {
			t.Fatalf("index %d is %q, want %q", i, got[i], want)
		}
	}
}

func TestMapPropagatesError(t *testing.T) {
	boom := errors.New("boom")

	_, err := Map(context.Background(), []int{1, 2, 3, 4}, 2, func(_ context.Context, n int) (int, error) {
		if n == 3 {
			return 0, boom
		}
		return n * 2, nil
	})
	if !errors.Is(err, boom) {
		t.Fatalf("got %v, want boom", err)
	}
}

func TestRunWorkersProcessesEverything(t *testing.T) {
	var p peak
	var sum atomic.Int64

	items := make([]int, 100)
	for i := range items {
		items[i] = i
	}

	RunWorkers(items, 5, func(n int) {
		p.enter()
		sum.Add(int64(n))
		p.leave()
	})

	if got := p.highest.Load(); got > 5 {
		t.Fatalf("peak concurrency %d, want at most 5", got)
	}
	if got := sum.Load(); got != 4950 {
		t.Fatalf("sum %d, want 4950", got)
	}
}
