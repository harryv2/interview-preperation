package main

import (
	"fmt"
	"sync"
)

// Define the callback signature. Using interface{} allows passing any payload.
type Callback func(payload interface{})

type EventBroker struct {
	mu          sync.RWMutex
	subscribers map[string][]Callback
}

func NewEventBroker() *EventBroker {
	return &EventBroker{
		subscribers: make(map[string][]Callback),
	}
}

// 1. Store the callback in the array (Subscribe)
func (b *EventBroker) Register(eventName string, cb Callback) {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.subscribers[eventName] = append(b.subscribers[eventName], cb)
}

// 2. Trigger them all together (Publish)
func (b *EventBroker) Trigger(eventName string, payload interface{}) {
	b.mu.RLock()

	// Check if anyone is actually listening
	cbs, exists := b.subscribers[eventName]
	if !exists || len(cbs) == 0 {
		b.mu.RUnlock()
		return
	}

	// THE TRAP FIX: Copy the slice so we can drop the lock immediately.
	// We do not want to hold the lock while executing unknown user functions.
	cbsCopy := make([]Callback, len(cbs))
	copy(cbsCopy, cbs)

	b.mu.RUnlock() // Drop the lock BEFORE executing!

	// ---------------------------------------------------------
	// Execute the callbacks.
	// (Using Goroutines + WaitGroup to fire them concurrently)
	// ---------------------------------------------------------
	var wg sync.WaitGroup
	for _, cb := range cbsCopy {
		wg.Add(1)

		// Capture the loop variable properly
		go func(callback Callback) {
			defer wg.Done()
			callback(payload)
		}(cb)
	}

	// Wait for all triggered callbacks to finish (if the requirements demand it)
	wg.Wait()
}

func main() {
	broker := NewEventBroker()

	// Register multiple callbacks to the same array
	broker.Register("user_signup", func(payload interface{}) {
		fmt.Printf("Email Service: Sending welcome email to %v\n", payload)
	})

	broker.Register("user_signup", func(payload interface{}) {
		fmt.Printf("Analytics Service: Tracking signup for %v\n", payload)
	})

	// Trigger them all at once
	fmt.Println("Triggering 'user_signup' event...")
	broker.Trigger("user_signup", "harish@example.com")
}
