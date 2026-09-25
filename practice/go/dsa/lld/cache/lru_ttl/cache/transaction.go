package cache

import (
	"errors"
	"time"
)

var ErrTxnFinished = errors.New("cache: transaction already finished")

type write[U any] struct {
	value   U
	ttl     time.Duration
	deleted bool
}

type Txn[T comparable, U any] struct {
	cache *LruCache[T, U]
	buf   map[T]write[U]

	order    []T
	finished bool
}

func (cache *LruCache[T, U]) Begin() *Txn[T, U] {
	return &Txn[T, U]{
		cache: cache,
		buf:   make(map[T]write[U]),
	}
}

func (cache *LruCache[T, U]) Transaction(fn func(tx *Txn[T, U]) error) error {
	tx := cache.Begin()
	defer tx.Rollback()

	if err := fn(tx); err != nil {
		return err
	}

	return tx.Commit()
}

func (tx *Txn[T, U]) Get(key T) (U, bool) {
	var zero U

	if tx.finished {
		return zero, false
	}

	w, buffered := tx.buf[key]

	switch {
	case buffered && w.deleted:
		return zero, false
	case buffered:
		return w.value, true
	default:
		return tx.cache.Get(key)
	}
}

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

	tx.order = append(tx.order, key)
	tx.buf[key] = w

	return nil
}

func (tx *Txn[T, U]) finalOrder() []T {
	lastAt := make(map[T]int, len(tx.buf))
	for i, key := range tx.order {
		lastAt[key] = i
	}

	final := make([]T, 0, len(tx.buf))
	for i, key := range tx.order {
		if lastAt[key] == i {
			final = append(final, key)
		}
	}

	return final
}

func (tx *Txn[T, U]) Commit() error {
	if tx.finished {
		return ErrTxnFinished
	}

	tx.cache.applyAll(tx.finalOrder(), tx.buf)
	tx.finish()

	return nil
}

func (tx *Txn[T, U]) Rollback() error {
	if tx.finished {
		return nil
	}

	tx.finish()

	return nil
}

func (tx *Txn[T, U]) finish() {
	tx.buf = nil
	tx.order = nil
	tx.finished = true
}
