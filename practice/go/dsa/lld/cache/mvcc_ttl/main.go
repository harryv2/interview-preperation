package main

import (
	"errors"
	"fmt"
	"sync"
	"time"

	"github.com/harryv2/interview-preperation/practice/go/dsa/lld/cache/mvcc_ttl/cache"
)

type fakeClock struct {
	mu  sync.Mutex
	now time.Time
}

func (c *fakeClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

func (c *fakeClock) Advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.now = c.now.Add(d)
}

func check(cond bool, msg string) {
	if !cond {
		panic(msg)
	}
}

func main() {
	clock := &fakeClock{now: time.Unix(0, 0)}
	c := cache.NewMVCCCache[string, string](clock, time.Hour)
	defer c.Close()

	c.Put("a", "1", time.Minute)

	tx := c.Begin()
	c.Put("a", "2", time.Minute)

	v, ok := tx.Get("a")
	check(ok && v == "1", "transaction sees its snapshot")
	v, ok = c.Get("a")
	check(ok && v == "2", "a fresh read sees the new version")
	check(tx.Rollback() == nil, "rollback")
	fmt.Println("snapshot read ok")

	c.Put("b", "1", 10*time.Second)

	long := c.Begin()
	clock.Advance(20 * time.Second)

	v, ok = long.Get("b")
	check(ok && v == "1", "alive as of the snapshot")
	_, ok = c.Get("b")
	check(!ok, "expired for anyone reading now")
	check(long.Rollback() == nil, "rollback")
	fmt.Println("ttl follows the snapshot ok")

	c.Put("k", "0", time.Minute)

	first := c.Begin()
	second := c.Begin()

	check(first.Put("k", "1", time.Minute) == nil, "stage first")
	check(first.Commit() == nil, "first commit")

	check(second.Put("k", "2", time.Minute) == nil, "stage second")
	check(errors.Is(second.Commit(), cache.ErrConflict), "second commit conflicts")

	v, ok = c.Get("k")
	check(ok && v == "1", "first committer won")
	fmt.Println("write-write conflict ok")

	err := c.Transaction(func(tx *cache.Txn[string, string]) error {
		check(tx.Put("x", "outer", time.Minute) == nil, "outer write")

		failed := tx.Nested(func(inner *cache.Txn[string, string]) error {
			check(inner.Depth() == 2, "inside a savepoint")
			check(inner.Put("x", "discarded", time.Minute) == nil, "inner write")
			return errors.New("validation failed")
		})
		check(failed != nil, "nested returns the block's error")
		check(tx.Depth() == 1, "frame unwound")

		v, ok := tx.Get("x")
		check(ok && v == "outer", "savepoint discarded")

		return tx.Nested(func(inner *cache.Txn[string, string]) error {
			return inner.Put("x", "kept", time.Minute)
		})
	})
	check(err == nil, "outer transaction commits")

	v, ok = c.Get("x")
	check(ok && v == "kept", "surviving savepoint merged and committed")
	fmt.Println("savepoints ok")

	err = c.Transaction(func(tx *cache.Txn[string, string]) error {
		check(tx.Put("y", "1", time.Minute) == nil, "write")
		_, visible := c.Get("y")
		check(!visible, "uncommitted write is invisible outside")
		return errors.New("boom")
	})
	check(err != nil, "helper surfaces the error")
	_, ok = c.Get("y")
	check(!ok, "helper rolled back")
	fmt.Println("auto-rollback ok")

	fmt.Println("all checks passed")
}
