package main

import (
	"fmt"
	"sync"
	"time"
)

// Callback defines the function signature for subscribers
type Callback func(payload interface{})

type EventBroker struct {
	mu          sync.RWMutex
	subscribers map[string][]Callback

	// The global queue where all triggered callbacks are sent
	taskQueue chan func()

	// Used for graceful shutdown of the worker pool
	workerWg sync.WaitGroup
}

// NewEventBroker boots up the global worker pool immediately
func NewEventBroker(concurrencyLimit int, maxQueueSize int) *EventBroker {
	broker := &EventBroker{
		subscribers: make(map[string][]Callback),
		taskQueue:   make(chan func(), maxQueueSize),
	}

	// Start the fixed number of background workers
	for i := 0; i < concurrencyLimit; i++ {
		broker.workerWg.Add(1)

		go func(workerID int) {
			defer broker.workerWg.Done()

			// Workers continuously pull tasks from the queue
			// This loop automatically terminates when taskQueue is closed
			for task := range broker.taskQueue {
				task()
			}
		}(i)
	}

	return broker
}

// Register safely adds a new callback to an event
func (b *EventBroker) Register(eventName string, cb Callback) {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.subscribers[eventName] = append(b.subscribers[eventName], cb)
}

// Trigger pushes all registered callbacks into the global worker pool
func (b *EventBroker) Trigger(eventName string, payload interface{}) {
	b.mu.RLock()
	cbs, exists := b.subscribers[eventName]
	if !exists || len(cbs) == 0 {
		b.mu.RUnlock()
		return
	}

	// Copy to drop the lock immediately
	cbsCopy := make([]Callback, len(cbs))
	copy(cbsCopy, cbs)
	b.mu.RUnlock()

	// Push tasks to the global queue.
	// If the queue is full, this naturally blocks the publisher (Backpressure).
	for _, cb := range cbsCopy {
		// Capture the exact callback and payload for the worker to execute
		// (Go 1.22+ handles the cb capture automatically, but explicit is safer for older versions)
		task := func(callback Callback, data interface{}) func() {
			return func() {
				callback(data)
			}
		}(cb, payload)

		b.taskQueue <- task
	}
}

// Shutdown gracefully stops the broker, ensuring all pending tasks finish
func (b *EventBroker) Shutdown() {
	close(b.taskQueue) // Signals workers to stop after draining the queue
	b.workerWg.Wait()  // Wait for all workers to finish their current tasks
}

// --- Demonstration ---

func main() {
	// Allow exactly 3 concurrent executions system-wide, with a queue buffer of 100
	broker := NewEventBroker(3, 100)

	// Register 5 slow callbacks to the same event
	for i := 1; i <= 5; i++ {
		workerNum := i
		broker.Register("heavy_computation", func(payload interface{}) {
			fmt.Printf("Executing callback %d for payload: %v\n", workerNum, payload)
			time.Sleep(1 * time.Second) // Simulate heavy work
			fmt.Printf("Finished callback %d\n", workerNum)
		})
	}

	fmt.Println("Triggering event. Watch how only 3 run at a time...")
	broker.Trigger("heavy_computation", "Dataset_A")

	// Trigger returns instantly because it just pushed to the buffered queue
	fmt.Println("Trigger function returned instantly to the publisher.")

	// Shut down gracefully (waits for the queue to drain)
	broker.Shutdown()
	fmt.Println("All events processed. System shut down.")
}
