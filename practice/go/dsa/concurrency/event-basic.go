package main

import "sync"

type PureDispatcher struct {
	mu              sync.Mutex
	cond            *sync.Cond
	activeCallbacks int
	eventRunning    bool
}

func NewPureDispatcher() *PureDispatcher {
	d := &PureDispatcher{}
	d.cond = sync.NewCond(&d.mu)
	return d
}

func (d *PureDispatcher) reg_cb(userCallback func()) {
	d.mu.Lock()

	// Wait if the massive system event is currently running
	for d.eventRunning {
		d.cond.Wait()
	}
	d.activeCallbacks++

	// CRITICAL: Drop the lock BEFORE executing user code.
	// If we hold it, and `userCallback` calls `reg_cb` again, we deadlock.
	d.mu.Unlock()

	userCallback()

	// Re-acquire to update state
	d.mu.Lock()
	d.activeCallbacks--
	if d.activeCallbacks == 0 {
		// Wake up the system event if it's waiting for drain
		d.cond.Broadcast()
	}
	d.mu.Unlock()
}

func (d *PureDispatcher) trigger_event(systemWork func()) {
	d.mu.Lock()
	d.eventRunning = true

	// Drain: Wait for in-flight callbacks to finish
	for d.activeCallbacks > 0 {
		d.cond.Wait()
	}

	// CRITICAL: Drop the lock for system work too, so we don't freeze the scheduler
	d.mu.Unlock()

	systemWork()

	d.mu.Lock()
	d.eventRunning = false
	d.cond.Broadcast() // Wake up all paused reg_cb threads
	d.mu.Unlock()
}
