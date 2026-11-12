package main

import "sync"

type CyclicBarrier struct {
	mu         sync.Mutex
	cond       *sync.Cond
	capacity   int // Total number of threads required to trip the barrier
	count      int // How many threads have arrived so far
	generation int // The "Epoch" - prevents fast threads from lapping slow threads
}

func NewCyclicBarrier(parties int) *CyclicBarrier {
	b := &CyclicBarrier{capacity: parties}
	b.cond = sync.NewCond(&b.mu)
	return b
}

// Await blocks the calling thread until all 'capacity' threads have called Await.
func (b *CyclicBarrier) Await() {
	b.mu.Lock()
	defer b.mu.Unlock()

	// 1. Snapshot the current generation for THIS specific thread
	myGeneration := b.generation
	b.count++

	// 2. Are we the final thread to arrive?
	if b.count == b.capacity {
		// Reset the counter for the next cycle
		b.count = 0

		// Advance the generation so sleeping threads know the round is over
		b.generation++

		// Wake up all other threads waiting at this barrier
		b.cond.Broadcast()
		return
	}

	// 3. We are NOT the final thread. Go to sleep.
	// We check `myGeneration == b.generation` to protect against spurious wakeups
	// AND to know when the final thread has officially advanced the epoch.
	for myGeneration == b.generation {
		b.cond.Wait()
	}
}
