package main

import (
	"fmt"
	"sync"
	"time"
)

type Callback func(payload interface{})

// 1. Isolate the state for each event name
type EventState struct {
	mu              sync.Mutex
	cond            *sync.Cond
	callbacks       []Callback
	activeExecs     int  // How many callbacks are currently executing
	sysEventRunning bool // Is the exclusive system event running?
}

type UnifiedBroker struct {
	globalMu sync.RWMutex
	events   map[string]*EventState
}

func NewUnifiedBroker() *UnifiedBroker {
	return &UnifiedBroker{
		events: make(map[string]*EventState),
	}
}

// 2. Lazy-load event states using Double-Checked Locking
func (b *UnifiedBroker) getEventState(name string) *EventState {
	b.globalMu.RLock()
	state, exists := b.events[name]
	b.globalMu.RUnlock()

	if exists {
		return state
	}

	b.globalMu.Lock()
	defer b.globalMu.Unlock()

	// Double-check in case another thread created it
	if state, exists = b.events[name]; exists {
		return state
	}

	newState := &EventState{
		callbacks: make([]Callback, 0),
	}
	newState.cond = sync.NewCond(&newState.mu)
	b.events[name] = newState
	return newState
}

// 3. Register stores the callback (Does not execute, so it never blocks on sysEvent)
func (b *UnifiedBroker) Register(eventName string, cb Callback) {
	state := b.getEventState(eventName)

	state.mu.Lock()
	defer state.mu.Unlock()
	state.callbacks = append(state.callbacks, cb)
}

// 4. Trigger fires all callbacks, but respects Pause/Drain state
func (b *UnifiedBroker) Trigger(eventName string, payload interface{}) {
	state := b.getEventState(eventName)

	state.mu.Lock()

	// A. Wait if a massive system event is running for this exact name
	for state.sysEventRunning {
		state.cond.Wait()
	}

	if len(state.callbacks) == 0 {
		state.mu.Unlock()
		return
	}

	// B. Copy callbacks to avoid deadlocking if user code calls Register()
	cbsCopy := make([]Callback, len(state.callbacks))
	copy(cbsCopy, state.callbacks)

	// C. Increment the active execution counter BEFORE dropping the lock
	state.activeExecs += len(cbsCopy)
	state.mu.Unlock()

	// D. Execute outside the lock
	for _, cb := range cbsCopy {
		go func(callback Callback) {
			// Run the user code
			callback(payload)

			// Safely decrement the counter when finished
			state.mu.Lock()
			state.activeExecs--
			if state.activeExecs == 0 {
				// Wake up RunSystemEvent if it is draining
				state.cond.Broadcast()
			}
			state.mu.Unlock()
		}(cb)
	}
}

// 5. Exclusive System Event (Halts new Triggers, Waits for active ones to Drain)
func (b *UnifiedBroker) RunSystemEvent(eventName string, sysWork func()) {
	state := b.getEventState(eventName)

	state.mu.Lock()
	fmt.Printf("[SYSTEM] Claiming '%s'. Halting new triggers.\n", eventName)
	state.sysEventRunning = true

	// Drain: Wait for currently executing callbacks to finish
	for state.activeExecs > 0 {
		fmt.Printf("[SYSTEM] Waiting for %d active '%s' callbacks to finish...\n", state.activeExecs, eventName)
		state.cond.Wait()
	}
	state.mu.Unlock()

	// Do exclusive work outside the lock
	sysWork()

	state.mu.Lock()
	fmt.Printf("[SYSTEM] Work complete for '%s'. Resuming triggers.\n", eventName)
	state.sysEventRunning = false
	state.cond.Broadcast() // Wake up any Triggers that arrived while we were working
	state.mu.Unlock()
}

func main() {
	broker := NewUnifiedBroker()

	// Register normal callbacks
	broker.Register("database_sync", func(payload interface{}) {
		fmt.Println("   -> Sync Callback: Started writing to DB...")
		time.Sleep(2 * time.Second) // Simulate slow write
		fmt.Println("   -> Sync Callback: Finished.")
	})

	// 1. Trigger the normal event
	fmt.Println("Triggering 'database_sync' (Takes 2 seconds)...")
	broker.Trigger("database_sync", "Row1")

	time.Sleep(500 * time.Millisecond) // Let the goroutine start

	// 2. Trigger the System Event (e.g., A massive Database Migration)
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		broker.RunSystemEvent("database_sync", func() {
			fmt.Println("[SYSTEM ***] EXECUTING EXCLUSIVE DB MIGRATION!")
			time.Sleep(1 * time.Second)
		})
	}()

	time.Sleep(500 * time.Millisecond)

	// 3. Try to trigger the event AGAIN while the system event is running
	// This will immediately block and wait until the migration finishes.
	wg.Add(1)
	go func() {
		defer wg.Done()
		fmt.Println("Attempting to trigger 'database_sync' again...")
		broker.Trigger("database_sync", "Row2")
		fmt.Println("Second trigger successfully dispatched!")
	}()

	wg.Wait()
}
