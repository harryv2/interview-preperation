// Package cache implements an MVCC cache: every key holds a chain of immutable
// versions, and a transaction reads a frozen snapshot of that chain rather than
// whatever the latest value happens to be.
//
// Reclamation is TTL plus version GC. There is no capacity bound and no LRU
// eviction, which removes the thing that made commit ordering matter in the LRU
// cache: with nothing to evict, the keys in one commit do not interact.
package cache

import (
	"errors"
	"sync"
	"time"
)

type Clock interface {
	Now() time.Time
}

type SystemClock struct{}

func (SystemClock) Now() time.Time {
	return time.Now()
}

var (
	ErrConflict = errors.New("mvcc: write-write conflict, transaction aborted")

	ErrTxnFinished = errors.New("mvcc: transaction already finished")
)

// version is one immutable revision of a key. Versions are appended and later
// reclaimed, never mutated, which is what lets readers run without blocking.
type version[U any] struct {
	value    U
	expiry   time.Time
	commitTS uint64
	deleted  bool // tombstone: the key was removed at this version
}

// snapshot is everything a reader needs, frozen at Begin.
//
// Two coordinates, not one. ts places the reader in the commit sequence. at
// places it in wall time, and that second one is why TTL and MVCC can coexist:
// expiry is judged against the snapshot's clock reading, so a key cannot expire
// out from under a transaction between two reads.
type snapshot struct {
	ts uint64
	at time.Time
}

type MVCCCache[T comparable, U any] struct {
	mu   sync.Mutex
	data map[T][]version[U] // ascending commitTS

	clock  Clock
	nextTS uint64 // last assigned commit timestamp

	nextTxnID uint64
	active    map[uint64]snapshot // open transactions, by id

	done      chan struct{}
	closeOnce sync.Once
}

func NewMVCCCache[T comparable, U any](clock Clock, gcEvery time.Duration) *MVCCCache[T, U] {
	c := &MVCCCache[T, U]{
		data:   make(map[T][]version[U]),
		clock:  clock,
		active: make(map[uint64]snapshot),
		done:   make(chan struct{}),
	}

	go c.runGC(gcEvery)

	return c
}

// Get reads through a snapshot taken right now, so a bare Get is a
// single-statement transaction.
func (c *MVCCCache[T, U]) Get(key T) (U, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()

	return c.visibleLocked(key, snapshot{ts: c.nextTS, at: c.clock.Now()})
}

func (c *MVCCCache[T, U]) Put(key T, value U, ttl time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.installLocked(map[T]write[U]{key: {value: value, ttl: ttl}})
}

func (c *MVCCCache[T, U]) Delete(key T) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.installLocked(map[T]write[U]{key: {deleted: true}})
}

func (c *MVCCCache[T, U]) Len() int {
	c.mu.Lock()
	defer c.mu.Unlock()

	snap := snapshot{ts: c.nextTS, at: c.clock.Now()}

	n := 0
	for key := range c.data {
		if _, ok := c.visibleLocked(key, snap); ok {
			n++
		}
	}

	return n
}

func (c *MVCCCache[T, U]) Close() {
	c.closeOnce.Do(func() { close(c.done) })
}

// visibleLocked walks the chain newest first and stops at the first version this
// snapshot may see. Three ways to miss: no version old enough, the visible one is
// a tombstone, or it had already expired when the snapshot was taken.
func (c *MVCCCache[T, U]) visibleLocked(key T, snap snapshot) (U, bool) {
	var zero U

	versions := c.data[key]

	for i := len(versions) - 1; i >= 0; i-- {
		v := versions[i]

		if v.commitTS > snap.ts {
			continue // committed after we started, invisible to us
		}
		if v.deleted {
			return zero, false
		}
		if v.expiry.Before(snap.at) {
			return zero, false
		}

		return v.value, true
	}

	return zero, false
}

// begin registers a snapshot. The id is separate from the timestamp because two
// transactions that start with no commit between them share a timestamp and
// would collide in the active set.
func (c *MVCCCache[T, U]) begin() (uint64, snapshot) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.nextTxnID++
	snap := snapshot{ts: c.nextTS, at: c.clock.Now()}
	c.active[c.nextTxnID] = snap

	return c.nextTxnID, snap
}

func (c *MVCCCache[T, U]) release(id uint64) {
	c.mu.Lock()
	defer c.mu.Unlock()

	delete(c.active, id)
}

// commit validates then installs, both under one lock, so nobody can slip a
// conflicting commit between the two halves.
func (c *MVCCCache[T, U]) commit(id uint64, snap snapshot, buf map[T]write[U]) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	delete(c.active, id)

	// Write-write conflict: someone committed to a key we are about to write,
	// after we took our snapshot. Versions ascend, so the newest one settles it.
	// This is snapshot isolation, not serializability: the read set is not
	// checked, so write skew is possible. See TestWriteSkewIsPossible.
	for key := range buf {
		versions := c.data[key]
		if n := len(versions); n > 0 && versions[n-1].commitTS > snap.ts {
			return ErrConflict
		}
	}

	c.installLocked(buf)

	return nil
}

// installLocked appends one version per key, all stamped with the same commit
// timestamp, so the batch becomes visible as a unit.
//
// Iteration order over buf is irrelevant here. Keys do not interact during a
// commit, because there is no capacity bound and so nothing evicts anything else.
func (c *MVCCCache[T, U]) installLocked(buf map[T]write[U]) {
	if len(buf) == 0 {
		return // read-only, do not burn a timestamp
	}

	c.nextTS++
	commitTS := c.nextTS
	now := c.clock.Now()

	for key, w := range buf {
		c.data[key] = append(c.data[key], version[U]{
			value:    w.value,
			expiry:   now.Add(w.ttl), // the TTL clock starts at commit, not at Put
			commitTS: commitTS,
			deleted:  w.deleted,
		})
	}
}

// horizonLocked is the oldest point anyone can still read from: the earliest
// snapshot held by an open transaction, in both coordinates. With nothing open it
// is the present, and everything superseded is collectable.
//
// An abandoned transaction pins this and stalls GC, which is the standard MVCC
// failure mode. Hence the scoped helpers in transaction.go.
func (c *MVCCCache[T, U]) horizonLocked() (watermark uint64, floor time.Time) {
	watermark = c.nextTS
	floor = c.clock.Now()

	for _, snap := range c.active {
		if snap.ts < watermark {
			watermark = snap.ts
		}
		if snap.at.Before(floor) {
			floor = snap.at
		}
	}

	return watermark, floor
}

// gc drops versions nobody can reach and keys that are dead for everyone.
func (c *MVCCCache[T, U]) gc() {
	c.mu.Lock()
	defer c.mu.Unlock()

	watermark, floor := c.horizonLocked()

	for key, versions := range c.data {
		// Newest version at or below the watermark. Everything older than it is
		// shadowed for all current and future readers.
		newest := -1
		for i, v := range versions {
			if v.commitTS > watermark {
				break
			}
			newest = i
		}

		if newest > 0 {
			versions = append(versions[:0], versions[newest:]...)
			c.data[key] = versions
		}

		// One version left, old enough that nothing newer is pending, and dead
		// either way: the key itself can go.
		if len(versions) == 1 {
			v := versions[0]
			if v.commitTS <= watermark && (v.deleted || v.expiry.Before(floor)) {
				delete(c.data, key)
			}
		}
	}
}

func (c *MVCCCache[T, U]) runGC(every time.Duration) {
	ticker := time.NewTicker(every)
	defer ticker.Stop()

	for {
		select {
		case <-c.done:
			return
		case <-ticker.C:
			c.gc()
		}
	}
}
