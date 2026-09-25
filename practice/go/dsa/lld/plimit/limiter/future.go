package limiter

import (
	"context"
	"sync"
)

// Future settles once, with the error the task returned, or with why it never ran.
type Future struct {
	done chan struct{}
	once sync.Once
	err  error
}

func newFuture() *Future {
	return &Future{done: make(chan struct{})}
}

// settle is idempotent so no path can close the channel twice.
func (f *Future) settle(err error) {
	f.once.Do(func() {
		f.err = err
		close(f.done)
	})
}

// Wait blocks for the task, or for the caller's own context to give up on it. Giving up on the
// wait does not cancel the task, only the waiting, which is why the task context is separate.
func (f *Future) Wait(ctx context.Context) error {
	select {
	case <-f.done:
		return f.err
	case <-ctx.Done():
		return ctx.Err()
	}
}

// Done closes when the task settles, for callers that want to select over several.
func (f *Future) Done() <-chan struct{} {
	return f.done
}

// Result carries a typed value alongside the future.
type Result[T any] struct {
	future *Future
	value  T
}

// Submit is a free function because Go has no generic methods. It keeps Limiter untyped and lets
// each call site name its own result type, instead of one limiter per type in the program.
func Submit[T any](l *Limiter, ctx context.Context, fn func(context.Context) (T, error)) (*Result[T], error) {
	result := &Result[T]{}

	future, err := l.Go(ctx, func(ctx context.Context) error {
		value, err := fn(ctx)
		result.value = value
		return err
	})
	if err != nil {
		return nil, err
	}

	result.future = future
	return result, nil
}

// Wait returns the value, or the zero value and the reason there isn't one.
func (r *Result[T]) Wait(ctx context.Context) (T, error) {
	if err := r.future.Wait(ctx); err != nil {
		var zero T
		return zero, err
	}
	return r.value, nil
}

// Done closes when the task settles.
func (r *Result[T]) Done() <-chan struct{} {
	return r.future.Done()
}
