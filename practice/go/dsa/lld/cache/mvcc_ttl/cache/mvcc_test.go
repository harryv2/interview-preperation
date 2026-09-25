package cache

import (
	"errors"
	"fmt"
	"sync"
	"testing"
	"time"
)

type testClock struct {
	mu  sync.Mutex
	now time.Time
}

func newTestClock() *testClock {
	return &testClock{now: time.Unix(0, 0)}
}

func (c *testClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

func (c *testClock) Advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.now = c.now.Add(d)
}

func newCache(t *testing.T) (*MVCCCache[string, string], *testClock) {
	t.Helper()

	clock := newTestClock()
	c := NewMVCCCache[string, string](clock, time.Hour)
	t.Cleanup(c.Close)

	return c, clock
}

func wantHit(t *testing.T, want string) func(string, bool) {
	t.Helper()

	return func(got string, ok bool) {
		t.Helper()
		if !ok {
			t.Fatalf("want hit %q, got a miss", want)
		}
		if got != want {
			t.Fatalf("want %q, got %q", want, got)
		}
	}
}

func wantMiss(t *testing.T) func(string, bool) {
	t.Helper()

	return func(got string, ok bool) {
		t.Helper()
		if ok {
			t.Fatalf("want a miss, got %q", got)
		}
	}
}

func TestReadYourWritesAndTombstones(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "1", time.Minute)

	tx := c.Begin()
	defer tx.Rollback()

	_ = tx.Put("b", "2", time.Minute)
	_ = tx.Delete("a")

	wantHit(t, "2")(tx.Get("b"))
	wantMiss(t)(tx.Get("a"))
	wantHit(t, "1")(c.Get("a"))
	wantMiss(t)(c.Get("b"))

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantMiss(t)(c.Get("a"))
	wantHit(t, "2")(c.Get("b"))
}

func TestSnapshotIsStableAgainstOtherCommits(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "1", time.Minute)

	tx := c.Begin()
	defer tx.Rollback()

	wantHit(t, "1")(tx.Get("a"))

	c.Put("a", "2", time.Minute)

	wantHit(t, "1")(tx.Get("a"))
	wantHit(t, "2")(c.Get("a"))
}

func TestTTLIsJudgedAgainstTheSnapshotNotWallClock(t *testing.T) {
	c, clock := newCache(t)
	c.Put("a", "1", 10*time.Second)

	tx := c.Begin()
	defer tx.Rollback()

	clock.Advance(20 * time.Second)

	wantHit(t, "1")(tx.Get("a"))
	wantMiss(t)(c.Get("a"))
}

func TestTTLClockStartsAtCommit(t *testing.T) {
	c, clock := newCache(t)

	tx := c.Begin()
	_ = tx.Put("a", "1", 10*time.Second)

	clock.Advance(8 * time.Second)

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	clock.Advance(10 * time.Second)
	wantHit(t, "1")(c.Get("a"))

	clock.Advance(time.Second)
	wantMiss(t)(c.Get("a"))
}

func TestWriteWriteConflictAbortsTheSecondCommitter(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "0", time.Minute)

	first := c.Begin()
	second := c.Begin()
	defer second.Rollback()

	_ = first.Put("a", "1", time.Minute)
	if err := first.Commit(); err != nil {
		t.Fatalf("first commit: %v", err)
	}

	_ = second.Put("a", "2", time.Minute)
	if err := second.Commit(); !errors.Is(err, ErrConflict) {
		t.Fatalf("second commit: want ErrConflict, got %v", err)
	}

	wantHit(t, "1")(c.Get("a"))
}

func TestDisjointWriteSetsDoNotConflict(t *testing.T) {
	c, _ := newCache(t)

	first := c.Begin()
	second := c.Begin()

	_ = first.Put("a", "1", time.Minute)
	_ = second.Put("b", "2", time.Minute)

	if err := first.Commit(); err != nil {
		t.Fatal(err)
	}
	if err := second.Commit(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(c.Get("a"))
	wantHit(t, "2")(c.Get("b"))
}

func TestWriteSkewIsPossible(t *testing.T) {
	c, _ := newCache(t)
	c.Put("on1", "true", time.Minute)
	c.Put("on2", "true", time.Minute)

	first := c.Begin()
	second := c.Begin()
	defer first.Rollback()
	defer second.Rollback()

	wantHit(t, "true")(first.Get("on1"))
	wantHit(t, "true")(first.Get("on2"))
	_ = first.Put("on1", "false", time.Minute)

	wantHit(t, "true")(second.Get("on1"))
	wantHit(t, "true")(second.Get("on2"))
	_ = second.Put("on2", "false", time.Minute)

	if err := first.Commit(); err != nil {
		t.Fatalf("first: %v", err)
	}

	if err := second.Commit(); err != nil {
		t.Fatalf("second: %v", err)
	}

	wantHit(t, "false")(c.Get("on1"))
	wantHit(t, "false")(c.Get("on2"))
}

func TestNestedMergesOnSuccess(t *testing.T) {
	c, _ := newCache(t)

	err := c.Transaction(func(tx *Txn[string, string]) error {
		_ = tx.Put("x", "outer", time.Minute)

		if err := tx.Nested(func(inner *Txn[string, string]) error {
			if got := inner.Depth(); got != 2 {
				t.Fatalf("want depth 2, got %d", got)
			}
			return inner.Put("x", "inner", time.Minute)
		}); err != nil {
			return err
		}

		if got := tx.Depth(); got != 1 {
			t.Fatalf("want depth 1 after the block, got %d", got)
		}
		wantHit(t, "inner")(tx.Get("x"))
		wantMiss(t)(c.Get("x"))

		return nil
	})
	if err != nil {
		t.Fatal(err)
	}

	wantHit(t, "inner")(c.Get("x"))
}

func TestNestedDiscardsOnError(t *testing.T) {
	c, _ := newCache(t)
	boom := errors.New("boom")

	err := c.Transaction(func(tx *Txn[string, string]) error {
		_ = tx.Put("x", "outer", time.Minute)

		if err := tx.Nested(func(inner *Txn[string, string]) error {
			_ = inner.Put("x", "inner", time.Minute)
			_ = inner.Delete("y")
			return boom
		}); !errors.Is(err, boom) {
			t.Fatalf("want boom out of Nested, got %v", err)
		}

		if got := tx.Depth(); got != 1 {
			t.Fatalf("want depth 1 after the failed block, got %d", got)
		}
		wantHit(t, "outer")(tx.Get("x"))

		return nil
	})
	if err != nil {
		t.Fatal(err)
	}

	wantHit(t, "outer")(c.Get("x"))
}

func TestNestedDiscardsOnPanic(t *testing.T) {
	c, _ := newCache(t)

	err := c.Transaction(func(tx *Txn[string, string]) error {
		_ = tx.Put("x", "outer", time.Minute)

		func() {
			defer func() {
				if recover() == nil {
					t.Error("want the panic to propagate")
				}
			}()

			_ = tx.Nested(func(inner *Txn[string, string]) error {
				_ = inner.Put("x", "inner", time.Minute)
				panic("boom")
			})
		}()

		if got := tx.Depth(); got != 1 {
			t.Fatalf("want depth 1 after the panic, got %d", got)
		}
		wantHit(t, "outer")(tx.Get("x"))

		return nil
	})
	if err != nil {
		t.Fatal(err)
	}

	wantHit(t, "outer")(c.Get("x"))
}

func TestDeepNestingUnwindsOneFrameAtATime(t *testing.T) {
	c, _ := newCache(t)

	err := c.Transaction(func(tx *Txn[string, string]) error {
		_ = tx.Put("w", "l1", time.Minute)

		return tx.Nested(func(l2 *Txn[string, string]) error {
			_ = l2.Put("w", "l2", time.Minute)

			_ = l2.Nested(func(l3 *Txn[string, string]) error {
				if got := l3.Depth(); got != 3 {
					t.Fatalf("want depth 3, got %d", got)
				}
				_ = l3.Delete("w")
				wantMiss(t)(l3.Get("w"))
				return errors.New("boom")
			})

			if got := l2.Depth(); got != 2 {
				t.Fatalf("want depth 2, got %d", got)
			}
			wantHit(t, "l2")(l2.Get("w"))

			return nil
		})
	})
	if err != nil {
		t.Fatal(err)
	}

	wantHit(t, "l2")(c.Get("w"))
}

func TestNestedSharesTheOuterSnapshot(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "1", time.Minute)

	tx := c.Begin()
	defer tx.Rollback()

	wantHit(t, "1")(tx.Get("a"))
	c.Put("a", "2", time.Minute)

	_ = tx.Nested(func(inner *Txn[string, string]) error {
		wantHit(t, "1")(inner.Get("a"))
		return nil
	})
}

func TestGCDropsSupersededVersions(t *testing.T) {
	c, _ := newCache(t)

	c.Put("a", "1", time.Minute)
	c.Put("a", "2", time.Minute)
	c.Put("a", "3", time.Minute)

	if got := len(c.data["a"]); got != 3 {
		t.Fatalf("want 3 versions before gc, got %d", got)
	}

	c.gc()

	if got := len(c.data["a"]); got != 1 {
		t.Fatalf("want 1 version after gc, got %d", got)
	}
	wantHit(t, "3")(c.Get("a"))
}

func TestGCKeepsVersionsAnOpenTransactionNeeds(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "1", time.Minute)

	tx := c.Begin()
	c.Put("a", "2", time.Minute)

	c.gc()

	if got := len(c.data["a"]); got != 2 {
		t.Fatalf("want both versions retained while a reader needs one, got %d", got)
	}
	wantHit(t, "1")(tx.Get("a"))

	if err := tx.Rollback(); err != nil {
		t.Fatal(err)
	}
	c.gc()

	if got := len(c.data["a"]); got != 1 {
		t.Fatalf("want 1 version after the reader left, got %d", got)
	}
}

func TestGCDropsTombstonedAndExpiredKeys(t *testing.T) {
	c, clock := newCache(t)

	c.Put("gone", "1", time.Minute)
	c.Delete("gone")

	c.Put("stale", "1", 10*time.Second)
	c.Put("fresh", "1", time.Hour)

	clock.Advance(20 * time.Second)
	c.gc()

	if _, ok := c.data["gone"]; ok {
		t.Fatal("tombstoned key should be reclaimed")
	}
	if _, ok := c.data["stale"]; ok {
		t.Fatal("expired key should be reclaimed")
	}
	wantHit(t, "1")(c.Get("fresh"))
}

func TestGCKeepsExpiredKeysAnOldSnapshotCanStillSee(t *testing.T) {
	c, clock := newCache(t)
	c.Put("a", "1", 10*time.Second)

	tx := c.Begin()
	clock.Advance(20 * time.Second)

	c.gc()

	if _, ok := c.data["a"]; !ok {
		t.Fatal("key reclaimed while an older snapshot can still read it")
	}
	wantHit(t, "1")(tx.Get("a"))

	if err := tx.Rollback(); err != nil {
		t.Fatal(err)
	}
	c.gc()

	if _, ok := c.data["a"]; ok {
		t.Fatal("key should be reclaimed once no snapshot can see it")
	}
}

func TestFinishedTransactionIsRejected(t *testing.T) {
	c, _ := newCache(t)

	tx := c.Begin()
	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	if err := tx.Put("a", "1", time.Minute); !errors.Is(err, ErrTxnFinished) {
		t.Fatalf("Put after commit: want ErrTxnFinished, got %v", err)
	}
	if err := tx.Delete("a"); !errors.Is(err, ErrTxnFinished) {
		t.Fatalf("Delete after commit: want ErrTxnFinished, got %v", err)
	}
	if err := tx.Nested(func(*Txn[string, string]) error { return nil }); !errors.Is(err, ErrTxnFinished) {
		t.Fatalf("Nested after commit: want ErrTxnFinished, got %v", err)
	}
	if err := tx.Commit(); !errors.Is(err, ErrTxnFinished) {
		t.Fatalf("second Commit: want ErrTxnFinished, got %v", err)
	}
	if err := tx.Rollback(); err != nil {
		t.Fatalf("Rollback after commit: want nil, got %v", err)
	}
}

func TestReadOnlyTransactionBurnsNoTimestamp(t *testing.T) {
	c, _ := newCache(t)
	c.Put("a", "1", time.Minute)

	before := c.nextTS

	if err := c.Transaction(func(tx *Txn[string, string]) error {
		tx.Get("a")
		return nil
	}); err != nil {
		t.Fatal(err)
	}

	if c.nextTS != before {
		t.Fatalf("read-only commit moved the clock from %d to %d", before, c.nextTS)
	}
	if len(c.active) != 0 {
		t.Fatalf("want no active transactions, got %d", len(c.active))
	}
}

func TestNoHalfAppliedTransactionIsObservable(t *testing.T) {
	c := NewMVCCCache[string, string](SystemClock{}, time.Hour)
	defer c.Close()

	const rounds = 5000

	var wg sync.WaitGroup
	stop := make(chan struct{})
	halfSeen := make(chan struct{}, 1)

	wg.Add(1)
	go func() {
		defer wg.Done()
		for {
			select {
			case <-stop:
				return
			default:
			}

			_ = c.Transaction(func(tx *Txn[string, string]) error {
				_, p := tx.Get("p")
				_, q := tx.Get("q")
				if p != q {
					select {
					case halfSeen <- struct{}{}:
					default:
					}
				}
				return nil
			})
		}
	}()

	for i := 0; i < rounds; i++ {
		_ = c.Transaction(func(tx *Txn[string, string]) error {
			_ = tx.Put("p", "1", time.Minute)
			_ = tx.Put("q", "2", time.Minute)
			return nil
		})
		_ = c.Transaction(func(tx *Txn[string, string]) error {
			_ = tx.Delete("p")
			_ = tx.Delete("q")
			return nil
		})
	}

	close(stop)
	wg.Wait()

	select {
	case <-halfSeen:
		t.Fatal("observed a half-applied transaction")
	default:
	}
}

func TestConcurrentTransactions(t *testing.T) {
	c := NewMVCCCache[string, string](SystemClock{}, 10*time.Millisecond)
	defer c.Close()

	const goroutines = 8
	const iterations = 200

	var wg sync.WaitGroup
	var conflicts int64
	var mu sync.Mutex

	for g := 0; g < goroutines; g++ {
		wg.Add(1)
		go func(g int) {
			defer wg.Done()
			for i := 0; i < iterations; i++ {
				key := fmt.Sprintf("k%d", i%8)

				err := c.Transaction(func(tx *Txn[string, string]) error {
					tx.Get(key)
					return tx.Nested(func(inner *Txn[string, string]) error {
						return inner.Put(key, fmt.Sprintf("g%d-%d", g, i), time.Minute)
					})
				})

				if err != nil && !errors.Is(err, ErrConflict) {
					t.Errorf("unexpected error: %v", err)
					return
				}
				if errors.Is(err, ErrConflict) {
					mu.Lock()
					conflicts++
					mu.Unlock()
				}
			}
		}(g)
	}

	for g := 0; g < goroutines; g++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := 0; i < iterations; i++ {
				c.Get(fmt.Sprintf("k%d", i%8))
				c.Len()
			}
		}()
	}

	wg.Wait()
	t.Logf("%d conflicts out of %d commits", conflicts, goroutines*iterations)
}
