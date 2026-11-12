package main

import (
	"errors"
	"fmt"
	"time"
)

// SendFlushCommand represents the network call to a single storage node
type SendFlushCommand func(nodeID int) error

func WaitForQuorum(nodes []int, requiredQuorum int, timeout time.Duration, flush SendFlushCommand) error {
	if requiredQuorum > len(nodes) {
		return errors.New("quorum is larger than available nodes")
	}

	// Buffer must equal total nodes to prevent goroutine leaks if we exit early
	results := make(chan error, len(nodes))

	// 1. Fan-out: Send flush to all nodes concurrently
	for _, nodeID := range nodes {
		go func(id int) {
			// Execute the network call and push the result to the channel
			results <- flush(id)
		}(nodeID)
	}

	successCount := 0
	failureCount := 0
	maxAllowedFailures := len(nodes) - requiredQuorum

	// 2. The Timeout Timer
	deadline := time.After(timeout)

	// 3. Fan-in: Wait for responses
	for {
		select {
		case err := <-results:
			if err == nil {
				successCount++
				if successCount >= requiredQuorum {
					return nil // Quorum reached! Return instantly.
				}
			} else {
				failureCount++
				// Fast-fail: If too many failed, quorum is mathematically impossible
				if failureCount > maxAllowedFailures {
					return fmt.Errorf("quorum impossible: %d nodes failed", failureCount)
				}
			}

		case <-deadline:
			// Timeout reached before quorum
			return fmt.Errorf("timed out waiting for quorum. Got %d/%d", successCount, requiredQuorum)
		}
	}
}
