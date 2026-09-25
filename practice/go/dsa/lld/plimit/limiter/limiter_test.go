package limiter

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func TestNeverExceedsConcurrency(t *testing.T) {
	const limit = 4
	pool := New(limit)

	var running, peak atomic.Int64

	for i := 0; i < 200; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
			now := running.Add(1)
			for {
				high := peak.Load()
				if now <= high || peak.CompareAndSwap(high, now) {
					break
				}
			}
			time.Sleep(time.Millisecond)
			running.Add(-1)
			return nil
		}); err != nil {
			t.Fatalf("submit: %v", err)
		}
	}

	pool.Wait()

	if got := peak.Load(); got > limit {
		t.Fatalf("ran %d at once, limit was %d", got, limit)
	}
	if got := peak.Load(); got != limit {
		t.Fatalf("never reached the limit, peak was %d", got)
	}
}

func TestAdmissionIsFIFO(t *testing.T) {
	pool := New(1)
	release := make(chan struct{})

	// occupy the only slot so everything after this queues in submission order
	blocker, err := pool.Go(context.Background(), func(ctx context.Context) error {
		<-release
		return nil
	})
	if err != nil {
		t.Fatalf("submit blocker: %v", err)
	}

	var mu sync.Mutex
	var order []int

	for i := 0; i < 50; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
			mu.Lock()
			order = append(order, i)
			mu.Unlock()
			return nil
		}); err != nil {
			t.Fatalf("submit %d: %v", i, err)
		}
	}

	close(release)
	if err := blocker.Wait(context.Background()); err != nil {
		t.Fatalf("blocker: %v", err)
	}
	pool.Wait()

	for i, got := range order {
		if got != i {
			t.Fatalf("position %d ran task %d, admission was not FIFO: %v", i, got, order)
		}
	}
}

func TestResultsAreTyped(t *testing.T) {
	pool := New(3)
	ctx := context.Background()

	handles := make([]*Result[int], 0, 20)
	for i := 0; i < 20; i++ {
		handle, err := Submit(pool, ctx, func(ctx context.Context) (int, error) {
			return i * i, nil
		})
		if err != nil {
			t.Fatalf("submit: %v", err)
		}
		handles = append(handles, handle)
	}

	for i, handle := range handles {
		got, err := handle.Wait(ctx)
		if err != nil {
			t.Fatalf("wait %d: %v", i, err)
		}
		if got != i*i {
			t.Fatalf("task %d returned %d, want %d", i, got, i*i)
		}
	}
}

func TestErrorReachesTheCaller(t *testing.T) {
	pool := New(2)
	sentinel := errors.New("boom")

	future, err := pool.Go(context.Background(), func(ctx context.Context) error {
		return sentinel
	})
	if err != nil {
		t.Fatalf("submit: %v", err)
	}

	if got := future.Wait(context.Background()); !errors.Is(got, sentinel) {
		t.Fatalf("got %v, want %v", got, sentinel)
	}
}

func TestPanicIsIsolated(t *testing.T) {
	pool := New(2)
	ctx := context.Background()

	bad, err := pool.Go(ctx, func(ctx context.Context) error {
		panic("bad input")
	})
	if err != nil {
		t.Fatalf("submit bad: %v", err)
	}

	var panicErr *PanicError
	if got := bad.Wait(ctx); !errors.As(got, &panicErr) {
		t.Fatalf("got %T %v, want *PanicError", got, got)
	}
	if panicErr.Value != "bad input" {
		t.Fatalf("panic value %v", panicErr.Value)
	}
	if len(panicErr.Stack) == 0 {
		t.Fatal("panic error carried no stack")
	}

	// the pool must still be usable, and the slot must have been given back
	good, err := pool.Go(ctx, func(ctx context.Context) error { return nil })
	if err != nil {
		t.Fatalf("submit good: %v", err)
	}
	if err := good.Wait(ctx); err != nil {
		t.Fatalf("pool died after a panic: %v", err)
	}

	pool.Wait()
	if pool.ActiveCount() != 0 {
		t.Fatalf("slot leaked after panic, %d still active", pool.ActiveCount())
	}
}

func TestQueuedTaskWithDeadContextNeverRuns(t *testing.T) {
	pool := New(1)
	release := make(chan struct{})

	blocker, err := pool.Go(context.Background(), func(ctx context.Context) error {
		<-release
		return nil
	})
	if err != nil {
		t.Fatalf("submit blocker: %v", err)
	}

	doomedCtx, cancel := context.WithCancel(context.Background())
	var ran atomic.Bool

	doomed, err := pool.Go(doomedCtx, func(ctx context.Context) error {
		ran.Store(true)
		return nil
	})
	if err != nil {
		t.Fatalf("submit doomed: %v", err)
	}

	cancel()
	close(release)
	_ = blocker.Wait(context.Background())

	if got := doomed.Wait(context.Background()); !errors.Is(got, context.Canceled) {
		t.Fatalf("got %v, want context.Canceled", got)
	}
	pool.Wait()
	if ran.Load() {
		t.Fatal("a task whose context died while queued still executed")
	}
}

func TestSetConcurrencyReleasesQueuedWork(t *testing.T) {
	pool := New(1)
	release := make(chan struct{})
	var started atomic.Int64

	for i := 0; i < 10; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
			started.Add(1)
			<-release
			return nil
		}); err != nil {
			t.Fatalf("submit: %v", err)
		}
	}

	waitFor(t, func() bool { return started.Load() == 1 }, "first task to start")
	if got := pool.PendingCount(); got != 9 {
		t.Fatalf("pending %d, want 9", got)
	}

	pool.SetConcurrency(5)
	waitFor(t, func() bool { return started.Load() == 5 }, "five tasks to be running")

	if got := pool.ActiveCount(); got != 5 {
		t.Fatalf("active %d, want 5", got)
	}

	close(release)
	pool.Wait()
}

func TestLoweringConcurrencyDoesNotInterrupt(t *testing.T) {
	pool := New(4)
	release := make(chan struct{})
	var started atomic.Int64

	for i := 0; i < 4; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
			started.Add(1)
			<-release
			return nil
		}); err != nil {
			t.Fatalf("submit: %v", err)
		}
	}
	waitFor(t, func() bool { return started.Load() == 4 }, "four tasks running")

	pool.SetConcurrency(1)
	if got := pool.ActiveCount(); got != 4 {
		t.Fatalf("lowering the limit interrupted work, active is %d, want 4", got)
	}

	close(release)
	pool.Wait()
}

func TestCloseRejectsNewWorkButDrainsTheQueue(t *testing.T) {
	pool := New(1)
	var done atomic.Int64

	for i := 0; i < 5; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
			time.Sleep(2 * time.Millisecond)
			done.Add(1)
			return nil
		}); err != nil {
			t.Fatalf("submit: %v", err)
		}
	}

	pool.Close()

	if _, err := pool.Go(context.Background(), func(ctx context.Context) error { return nil }); !errors.Is(err, ErrClosed) {
		t.Fatalf("got %v, want ErrClosed", err)
	}

	pool.Wait()
	if got := done.Load(); got != 5 {
		t.Fatalf("close dropped queued work, %d of 5 ran", got)
	}
}

func TestClearQueueDropsPending(t *testing.T) {
	pool := New(1)
	release := make(chan struct{})

	blocker, err := pool.Go(context.Background(), func(ctx context.Context) error {
		<-release
		return nil
	})
	if err != nil {
		t.Fatalf("submit blocker: %v", err)
	}

	futures := make([]*Future, 0, 6)
	for i := 0; i < 6; i++ {
		future, err := pool.Go(context.Background(), func(ctx context.Context) error {
			t.Error("a dropped task ran")
			return nil
		})
		if err != nil {
			t.Fatalf("submit: %v", err)
		}
		futures = append(futures, future)
	}

	if got := pool.ClearQueue(); got != 6 {
		t.Fatalf("dropped %d, want 6", got)
	}

	for i, future := range futures {
		if got := future.Wait(context.Background()); !errors.Is(got, ErrDropped) {
			t.Fatalf("future %d settled with %v, want ErrDropped", i, got)
		}
	}

	close(release)
	if err := blocker.Wait(context.Background()); err != nil {
		t.Fatalf("clearing the queue disturbed the running task: %v", err)
	}
	pool.Wait()
}

func TestMaxQueueIsBackpressure(t *testing.T) {
	pool := New(1, WithMaxQueue(2))
	release := make(chan struct{})

	if _, err := pool.Go(context.Background(), func(ctx context.Context) error {
		<-release
		return nil
	}); err != nil {
		t.Fatalf("submit blocker: %v", err)
	}
	waitFor(t, func() bool { return pool.ActiveCount() == 1 }, "blocker to start")

	for i := 0; i < 2; i++ {
		if _, err := pool.Go(context.Background(), func(ctx context.Context) error { return nil }); err != nil {
			t.Fatalf("submit %d should have fit in the queue: %v", i, err)
		}
	}

	if _, err := pool.Go(context.Background(), func(ctx context.Context) error { return nil }); !errors.Is(err, ErrQueueFull) {
		t.Fatalf("got %v, want ErrQueueFull", err)
	}

	close(release)
	pool.Wait()
}

func TestWaitReturnsImmediatelyWhenIdle(t *testing.T) {
	pool := New(2)
	done := make(chan struct{})

	go func() {
		pool.Wait()
		close(done)
	}()

	select {
	case <-done:
	case <-time.After(time.Second):
		t.Fatal("Wait blocked on an idle limiter")
	}
}

func TestMapKeepsInputOrder(t *testing.T) {
	items := make([]int, 100)
	for i := range items {
		items[i] = i
	}

	got, err := Map(context.Background(), 8, items, func(ctx context.Context, n int) (string, error) {
		time.Sleep(time.Duration(n%5) * time.Millisecond)
		return fmt.Sprintf("v%d", n), nil
	})
	if err != nil {
		t.Fatalf("map: %v", err)
	}

	for i, value := range got {
		if want := fmt.Sprintf("v%d", i); value != want {
			t.Fatalf("position %d holds %q, want %q", i, value, want)
		}
	}
}

func TestMapFirstErrorCancelsTheRest(t *testing.T) {
	sentinel := errors.New("item 3 is bad")
	var completed atomic.Int64

	_, err := Map(context.Background(), 2, []int{1, 2, 3, 4, 5, 6, 7, 8}, func(ctx context.Context, n int) (int, error) {
		if n == 3 {
			return 0, sentinel
		}
		select {
		case <-ctx.Done():
			return 0, ctx.Err()
		case <-time.After(50 * time.Millisecond):
			completed.Add(1)
			return n * n, nil
		}
	})

	if !errors.Is(err, sentinel) {
		t.Fatalf("got %v, want %v", err, sentinel)
	}
	if got := completed.Load(); got > 4 {
		t.Fatalf("%d tasks ran to completion, the cancel did not propagate", got)
	}
}

func TestMapRespectsConcurrency(t *testing.T) {
	var running, peak atomic.Int64

	items := make([]int, 60)
	_, err := Map(context.Background(), 5, items, func(ctx context.Context, _ int) (int, error) {
		now := running.Add(1)
		for {
			high := peak.Load()
			if now <= high || peak.CompareAndSwap(high, now) {
				break
			}
		}
		time.Sleep(time.Millisecond)
		running.Add(-1)
		return 0, nil
	})
	if err != nil {
		t.Fatalf("map: %v", err)
	}
	if got := peak.Load(); got > 5 {
		t.Fatalf("map ran %d at once, limit was 5", got)
	}
}

func TestConcurrentSubmittersDoNotRace(t *testing.T) {
	pool := New(8)
	var wg sync.WaitGroup
	var completed atomic.Int64

	for i := 0; i < 50; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < 20; j++ {
				future, err := pool.Go(context.Background(), func(ctx context.Context) error {
					completed.Add(1)
					return nil
				})
				if err != nil {
					t.Error(err)
					return
				}
				_ = future.Wait(context.Background())
			}
		}()
	}

	wg.Wait()
	pool.Wait()

	if got := completed.Load(); got != 1000 {
		t.Fatalf("%d tasks completed, want 1000", got)
	}
	if pool.ActiveCount() != 0 || pool.PendingCount() != 0 {
		t.Fatalf("limiter did not settle: %d active, %d pending", pool.ActiveCount(), pool.PendingCount())
	}
}

func waitFor(t *testing.T, cond func() bool, what string) {
	t.Helper()

	deadline := time.Now().Add(2 * time.Second)
	for time.Now().Before(deadline) {
		if cond() {
			return
		}
		time.Sleep(time.Millisecond)
	}
	t.Fatalf("timed out waiting for %s", what)
}
