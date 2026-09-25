package cache

import "time"

// write is one buffered mutation. deleted marks a tombstone; a key absent from
// the frame means this scope never touched it, so the read falls through to the
// enclosing scope. Collapsing those two states is the classic bug here.
type write[U any] struct {
	value   U
	ttl     time.Duration
	deleted bool
}

// Txn is a snapshot plus a stack of write frames.
//
// Reads come from the newest frame that mentions the key, then from the
// transaction's snapshot. Nothing is locked while a transaction is open, so
// readers and writers never block each other; the price is that a commit can be
// rejected with ErrConflict.
//
// Frames are savepoints, entered only through Nested, so they cannot leak and the
// outermost Commit is never ambiguous about what it is committing.
//
// A Txn is not safe for concurrent use by multiple goroutines.
type Txn[T comparable, U any] struct {
	cache    *MVCCCache[T, U]
	id       uint64
	snap     snapshot
	frames   []map[T]write[U]
	finished bool
}

// Begin takes a snapshot. Prefer Transaction; with Begin, always
// `defer tx.Rollback()`, because an unfinished transaction pins the GC horizon.
func (c *MVCCCache[T, U]) Begin() *Txn[T, U] {
	id, snap := c.begin()

	return &Txn[T, U]{
		cache:  c,
		id:     id,
		snap:   snap,
		frames: []map[T]write[U]{make(map[T]write[U])},
	}
}

// Transaction runs fn, committing when it returns nil and rolling back
// otherwise, panics included. The commit may still fail with ErrConflict.
func (c *MVCCCache[T, U]) Transaction(fn func(tx *Txn[T, U]) error) error {
	tx := c.Begin()
	defer tx.Rollback()

	if err := fn(tx); err != nil {
		return err
	}

	return tx.Commit()
}

func (tx *Txn[T, U]) Depth() int {
	return len(tx.frames)
}

// Get reads the newest frame that mentions the key, then this transaction's
// snapshot. Repeating a Get gives the same answer for the life of the
// transaction, TTL expiry included.
//
// Get on a finished transaction reports a miss; the write methods return
// ErrTxnFinished, but Get has no error to return.
func (tx *Txn[T, U]) Get(key T) (U, bool) {
	var zero U

	if tx.finished {
		return zero, false
	}

	for i := len(tx.frames) - 1; i >= 0; i-- {
		w, buffered := tx.frames[i][key]
		if !buffered {
			continue // untouched at this level, look deeper
		}
		if w.deleted {
			return zero, false
		}
		return w.value, true
	}

	tx.cache.mu.Lock()
	defer tx.cache.mu.Unlock()

	return tx.cache.visibleLocked(key, tx.snap)
}

// Put buffers a write. The ttl is relative because the version does not exist
// until commit, so its clock starts there.
func (tx *Txn[T, U]) Put(key T, value U, ttl time.Duration) error {
	return tx.stage(key, write[U]{value: value, ttl: ttl})
}

func (tx *Txn[T, U]) Delete(key T) error {
	return tx.stage(key, write[U]{deleted: true})
}

func (tx *Txn[T, U]) stage(key T, w write[U]) error {
	if tx.finished {
		return ErrTxnFinished
	}

	tx.frames[len(tx.frames)-1][key] = w

	return nil
}

// Nested runs fn against a savepoint. Its writes merge into the enclosing scope
// when fn returns nil, and are discarded when it returns an error or panics.
// Either way nothing reaches the cache; only the outermost Commit does that.
//
// The savepoint shares the transaction's snapshot. A savepoint is a scope for
// undo, not a new point in time.
func (tx *Txn[T, U]) Nested(fn func(tx *Txn[T, U]) error) error {
	if tx.finished {
		return ErrTxnFinished
	}

	tx.frames = append(tx.frames, make(map[T]write[U]))
	depth := len(tx.frames)

	keep := false

	// Deferred so a panic unwinds the frame too, leaving the transaction at the
	// depth it had on entry rather than half open.
	defer func() {
		frame := tx.frames[depth-1]
		tx.frames = tx.frames[:depth-1]

		if !keep {
			return
		}

		parent := tx.frames[len(tx.frames)-1]
		for key, w := range frame {
			parent[key] = w
		}
	}()

	if err := fn(tx); err != nil {
		return err
	}

	keep = true

	return nil
}

// Commit installs the buffered writes as one new version per key, sharing a
// commit timestamp. It fails with ErrConflict if another transaction wrote any of
// those keys after this one snapshotted; either way the transaction is over.
func (tx *Txn[T, U]) Commit() error {
	if tx.finished {
		return ErrTxnFinished
	}
	if len(tx.frames) != 1 {
		panic("mvcc: Commit at depth > 1, a Nested block did not unwind")
	}

	err := tx.cache.commit(tx.id, tx.snap, tx.frames[0])
	tx.finish()

	return err
}

// Rollback drops the buffer and releases the snapshot so GC can move on. Safe to
// defer straight after Begin: rolling back a finished transaction is a no-op.
func (tx *Txn[T, U]) Rollback() error {
	if tx.finished {
		return nil
	}

	tx.cache.release(tx.id)
	tx.finish()

	return nil
}

func (tx *Txn[T, U]) finish() {
	tx.frames = nil
	tx.finished = true
}
