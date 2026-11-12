package main

import "sync"

type BoundedQueue struct {
	mu       sync.Mutex
	notFull  *sync.Cond
	notEmpty *sync.Cond
	items    []int
	capacity int
}

func NewBoundedQueue(capacity int) *BoundedQueue {
	q := &BoundedQueue{
		capacity: capacity,
		items:    make([]int, 0, capacity),
	}
	// Both conditions share the EXACT SAME Mutex
	q.notFull = sync.NewCond(&q.mu)
	q.notEmpty = sync.NewCond(&q.mu)
	return q
}

func (q *BoundedQueue) Enqueue(val int) {
	q.mu.Lock()
	defer q.mu.Unlock()

	// Wait while the queue is full
	for len(q.items) == q.capacity {
		q.notFull.Wait()
	}

	q.items = append(q.items, val)

	// Wake up EXACTLY ONE waiting consumer
	q.notEmpty.Signal()
}

func (q *BoundedQueue) Dequeue() int {
	q.mu.Lock()
	defer q.mu.Unlock()

	// Wait while the queue is empty
	for len(q.items) == 0 {
		q.notEmpty.Wait()
	}

	val := q.items[0]
	q.items = q.items[1:] // In production, use a ring buffer here to avoid slice shifting

	// Wake up EXACTLY ONE waiting producer
	q.notFull.Signal()

	return val
}
