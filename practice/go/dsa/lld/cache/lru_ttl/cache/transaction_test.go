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

func newCache(t *testing.T, maxSize int) (*LruCache[string, string], *testClock) {
	t.Helper()

	clock := newTestClock()
	c := NewLruCache[string, string](maxSize, clock)
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

func TestReadYourWritesAndInvisibilityBeforeCommit(t *testing.T) {
	c, _ := newCache(t, 4)

	tx := c.Begin()
	if err := tx.Put("a", "1", time.Minute); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(tx.Get("a"))
	wantMiss(t)(c.Get("a"))

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(c.Get("a"))
}

func TestDeleteIsATombstoneNotAnAbsence(t *testing.T) {
	c, _ := newCache(t, 4)
	c.Add("a", "1", time.Minute)

	tx := c.Begin()
	if err := tx.Delete("a"); err != nil {
		t.Fatal(err)
	}

	wantMiss(t)(tx.Get("a"))
	wantHit(t, "1")(c.Get("a"))

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantMiss(t)(c.Get("a"))
}

func TestUntouchedKeysFallThroughToCommittedState(t *testing.T) {
	c, _ := newCache(t, 4)
	c.Add("a", "1", time.Minute)

	tx := c.Begin()
	if err := tx.Put("b", "2", time.Minute); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(tx.Get("a"))
}

func TestRollbackDiscardsEverything(t *testing.T) {
	c, _ := newCache(t, 4)
	c.Add("a", "1", time.Minute)

	tx := c.Begin()
	_ = tx.Put("b", "2", time.Minute)
	_ = tx.Delete("a")

	if err := tx.Rollback(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(c.Get("a"))
	wantMiss(t)(c.Get("b"))
}

func TestTTLClockStartsAtCommit(t *testing.T) {
	c, clock := newCache(t, 4)

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

func TestTransactionHelperCommitsOnNil(t *testing.T) {
	c, _ := newCache(t, 4)

	err := c.Transaction(func(tx *Txn[string, string]) error {
		return tx.Put("a", "1", time.Minute)
	})
	if err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1")(c.Get("a"))
}

func TestTransactionHelperRollsBackOnError(t *testing.T) {
	c, _ := newCache(t, 4)
	boom := errors.New("boom")

	err := c.Transaction(func(tx *Txn[string, string]) error {
		_ = tx.Put("a", "1", time.Minute)
		return boom
	})
	if !errors.Is(err, boom) {
		t.Fatalf("want boom, got %v", err)
	}

	wantMiss(t)(c.Get("a"))
}

func TestTransactionHelperRollsBackOnPanic(t *testing.T) {
	c, _ := newCache(t, 4)

	func() {
		defer func() {
			if recover() == nil {
				t.Error("want the panic to propagate")
			}
		}()

		_ = c.Transaction(func(tx *Txn[string, string]) error {
			_ = tx.Put("a", "1", time.Minute)
			panic("boom")
		})
	}()

	wantMiss(t)(c.Get("a"))
}

func TestCommitOrderIsDeterministicUnderEviction(t *testing.T) {
	for attempt := 0; attempt < 20; attempt++ {
		c, _ := newCache(t, 3)

		tx := c.Begin()
		for i := 1; i <= 5; i++ {
			_ = tx.Put(fmt.Sprintf("k%d", i), fmt.Sprintf("v%d", i), time.Minute)
		}
		if err := tx.Commit(); err != nil {
			t.Fatal(err)
		}

		for i := 1; i <= 2; i++ {
			if _, ok := c.Get(fmt.Sprintf("k%d", i)); ok {
				t.Fatalf("attempt %d: k%d should have been evicted", attempt, i)
			}
		}
		for i := 3; i <= 5; i++ {
			wantHit(t, fmt.Sprintf("v%d", i))(c.Get(fmt.Sprintf("k%d", i)))
		}
	}
}

func TestRepeatedWriteResolvesToTheLastValue(t *testing.T) {
	c, _ := newCache(t, 4)

	tx := c.Begin()
	_ = tx.Put("a", "first", time.Minute)
	_ = tx.Put("b", "2", time.Minute)
	_ = tx.Put("a", "second", time.Minute)
	_ = tx.Delete("b")
	_ = tx.Put("b", "3", time.Minute)

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "second")(c.Get("a"))
	wantHit(t, "3")(c.Get("b"))
}

func TestRewriteMovesTheKeyToMostRecent(t *testing.T) {
	c, _ := newCache(t, 2)

	tx := c.Begin()
	_ = tx.Put("a", "1", time.Minute)
	_ = tx.Put("b", "2", time.Minute)
	_ = tx.Put("a", "1-updated", time.Minute)
	_ = tx.Put("c", "3", time.Minute)

	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "1-updated")(c.Get("a"))
	wantMiss(t)(c.Get("b"))
	wantHit(t, "3")(c.Get("c"))
}

func TestCommitMatchesSequentialWrites(t *testing.T) {
	writes := []struct {
		key, value string
		del        bool
	}{
		{key: "a", value: "1"},
		{key: "b", value: "2"},
		{key: "a", value: "1b"},
		{key: "c", value: "3"},
		{key: "b", del: true},
		{key: "d", value: "4"},
		{key: "a", value: "1c"},
	}

	sequential, _ := newCache(t, 3)
	for _, w := range writes {
		if w.del {
			sequential.Delete(w.key)
			continue
		}
		sequential.Add(w.key, w.value, time.Minute)
	}

	transactional, _ := newCache(t, 3)
	tx := transactional.Begin()
	for _, w := range writes {
		if w.del {
			_ = tx.Delete(w.key)
			continue
		}
		_ = tx.Put(w.key, w.value, time.Minute)
	}
	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	for _, key := range []string{"a", "b", "c", "d"} {
		wantValue, wantOK := sequential.Get(key)
		gotValue, gotOK := transactional.Get(key)

		if wantOK != gotOK || wantValue != gotValue {
			t.Fatalf("key %q: sequential gave (%q,%v), transaction gave (%q,%v)",
				key, wantValue, wantOK, gotValue, gotOK)
		}
	}
}

func TestCommitBeatsAnExpiredKey(t *testing.T) {
	c, clock := newCache(t, 4)
	c.Add("a", "old", time.Second)

	tx := c.Begin()
	clock.Advance(2 * time.Second)

	wantMiss(t)(tx.Get("a"))

	_ = tx.Put("a", "new", time.Minute)
	if err := tx.Commit(); err != nil {
		t.Fatal(err)
	}

	wantHit(t, "new")(c.Get("a"))
}

func TestFinishedTransactionIsRejected(t *testing.T) {
	c, _ := newCache(t, 4)

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
	if err := tx.Commit(); !errors.Is(err, ErrTxnFinished) {
		t.Fatalf("second Commit: want ErrTxnFinished, got %v", err)
	}

	if err := tx.Rollback(); err != nil {
		t.Fatalf("Rollback after commit: want nil, got %v", err)
	}
}

func TestConcurrentCommitsAndReads(t *testing.T) {
	c := NewLruCache[string, string](32, SystemClock{})
	defer c.Close()

	const goroutines = 8
	const iterations = 200

	var wg sync.WaitGroup

	for g := 0; g < goroutines; g++ {
		wg.Add(1)
		go func(g int) {
			defer wg.Done()
			for i := 0; i < iterations; i++ {
				key := fmt.Sprintf("k%d", i%16)
				_ = c.Transaction(func(tx *Txn[string, string]) error {
					_ = tx.Put(key, fmt.Sprintf("g%d-%d", g, i), time.Minute)
					_ = tx.Delete(fmt.Sprintf("k%d", (i+1)%16))
					tx.Get(key)
					return nil
				})
			}
		}(g)
	}

	for g := 0; g < goroutines; g++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := 0; i < iterations; i++ {
				key := fmt.Sprintf("k%d", i%16)
				c.Get(key)
				c.Contains(key)
				c.Size()
			}
		}()
	}

	wg.Wait()
}
