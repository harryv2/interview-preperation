package main

import (
	"fmt"
	"sync"
	"time"

	"github.com/harryv2/interview-preperation/practice/go/dsa/lld/cache/lru_ttl/cache"
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
	lru := cache.NewLruCache[string, string](2, clock)
	defer lru.Close()

	lru.Add("a", "1", 30*time.Second)
	v, ok := lru.Get("a")
	check(ok && v == "1", "get after add")

	clock.Advance(30 * time.Second)
	v, ok = lru.Get("a")
	check(ok && v == "1", "alive at exactly ttl")

	clock.Advance(time.Second)
	_, ok = lru.Get("a")
	check(!ok, "expired after ttl")
	check(!lru.Contains("a"), "contains after expiry")
	check(lru.Size() == 0, "size after expiry")
	fmt.Println("expiry on get ok")

	lru.Add("a", "1", time.Minute)
	lru.Add("b", "2", time.Minute)
	lru.Get("a")
	lru.Add("c", "3", time.Minute)
	_, ok = lru.Get("b")
	check(!ok, "b evicted as lru")
	_, okA := lru.Get("a")
	_, okC := lru.Get("c")
	check(okA && okC && lru.Size() == 2, "a and c remain")
	fmt.Println("lru eviction ok")

	lru.Add("a", "11", 5*time.Second)
	clock.Advance(4 * time.Second)
	v, ok = lru.Get("a")
	check(ok && v == "11", "updated value before new ttl")
	clock.Advance(2 * time.Second)
	_, ok = lru.Get("a")
	check(!ok, "expired after new ttl")
	fmt.Println("ttl reset on update ok")

	lru.Add("x", "9", time.Second)
	lru.Add("y", "8", time.Second)
	clock.Advance(2 * time.Second)
	check(lru.Size() == 2, "expired entries still counted before sweep")
	time.Sleep(2500 * time.Millisecond)
	check(lru.Size() == 0, "sweeper removed expired entries")
	fmt.Println("background sweep ok")

	fmt.Println("all checks passed")
}
