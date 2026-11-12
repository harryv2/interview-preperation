package main

import (
	"errors"
	"fmt"
	"sync"
	"time"
)

type Priority int

const (
	High Priority = iota
	Medium
	Low
)

func (p Priority) String() string {
	return [...]string{"HIGH", "MEDIUM", "LOW"}[p]
}

var ErrClosed = errors.New("runner is closed")

type task struct {
	name        string
	seq         int64
	submittedAt time.Time
	run         func()
}

type Runner struct {
	maxWait time.Duration

	mu        sync.Mutex
	hasWork   *sync.Cond
	queues    [3][]*task
	accepting bool
	nextSeq   int64

	workers sync.WaitGroup
}

func NewRunner(workers int, maxWait time.Duration) *Runner {
	if workers <= 0 {
		panic("need at least one worker")
	}
	r := &Runner{maxWait: maxWait, accepting: true}
	r.hasWork = sync.NewCond(&r.mu)
	r.workers.Add(workers)
	for i := 0; i < workers; i++ {
		go r.workLoop()
	}
	return r
}

type Future[T any] struct {
	done  chan struct{}
	value T
	err   error
}

func (f *Future[T]) Wait() (T, error) {
	<-f.done
	return f.value, f.err
}

func Submit[T any](r *Runner, name string, priority Priority, work func() (T, error)) (*Future[T], error) {
	f := &Future[T]{done: make(chan struct{})}

	r.mu.Lock()
	defer r.mu.Unlock()

	if !r.accepting {
		return nil, ErrClosed
	}
	t := &task{
		name:        name,
		seq:         r.nextSeq,
		submittedAt: time.Now(),
		run: func() {
			defer close(f.done)
			defer func() {
				if p := recover(); p != nil {
					f.err = fmt.Errorf("task %s panicked: %v", name, p)
				}
			}()
			f.value, f.err = work()
		},
	}
	r.nextSeq++
	r.queues[priority] = append(r.queues[priority], t)
	r.hasWork.Signal()
	return f, nil
}

func (r *Runner) Close() {
	r.mu.Lock()
	r.accepting = false
	r.hasWork.Broadcast()
	r.mu.Unlock()

	r.workers.Wait()
}

func (r *Runner) workLoop() {
	defer r.workers.Done()
	for {
		t := r.next()
		if t == nil {
			return
		}
		t.run()
	}
}

func (r *Runner) next() *task {
	r.mu.Lock()
	defer r.mu.Unlock()

	for r.empty() {
		if !r.accepting {
			return nil
		}
		r.hasWork.Wait()
	}

	now := time.Now()
	starved := -1
	for p, q := range r.queues {
		if len(q) == 0 || now.Sub(q[0].submittedAt) < r.maxWait {
			continue
		}
		if starved == -1 || q[0].seq < r.queues[starved][0].seq {
			starved = p
		}
	}
	if starved != -1 {
		return r.pop(starved)
	}

	for p, q := range r.queues {
		if len(q) > 0 {
			return r.pop(p)
		}
	}
	return nil
}

func (r *Runner) empty() bool {
	for _, q := range r.queues {
		if len(q) > 0 {
			return false
		}
	}
	return true
}

func (r *Runner) pop(p int) *task {
	t := r.queues[p][0]
	r.queues[p][0] = nil
	r.queues[p] = r.queues[p][1:]
	return t
}
