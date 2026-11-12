package main

import (
	"fmt"
	"sync"
)

type ThreadBounce struct {
	n    int
	mu   sync.Mutex
	cond *sync.Cond

	// The State Variable dictating whose turn it is
	isFooTurn bool
}

func NewThreadBounce(n int) *ThreadBounce {
	b := &ThreadBounce{
		n:         n,
		isFooTurn: true, // Foo always starts
	}
	b.cond = sync.NewCond(&b.mu)
	return b
}

func (b *ThreadBounce) Foo() {
	for i := 0; i < b.n; i++ {
		b.mu.Lock()

		// 1. Wait if it is NOT Foo's turn
		for !b.isFooTurn {
			b.cond.Wait()
		}

		// 2. Execute the work
		fmt.Print("Foo")

		// 3. Bounce the state to the other thread
		b.isFooTurn = false

		// 4. Wake up the waiting thread
		b.cond.Signal()

		b.mu.Unlock()
	}
}

func (b *ThreadBounce) Bar() {
	for i := 0; i < b.n; i++ {
		b.mu.Lock()

		// 1. Wait if it is NOT Bar's turn (i.e., it IS Foo's turn)
		for b.isFooTurn {
			b.cond.Wait()
		}

		// 2. Execute the work
		fmt.Println("Bar")

		// 3. Bounce the state back to the first thread
		b.isFooTurn = true

		// 4. Wake up the waiting thread
		b.cond.Signal()

		b.mu.Unlock()
	}
}

func main() {
	bounce := NewThreadBounce(5)
	var wg sync.WaitGroup

	wg.Add(2)

	// Even if Bar starts first, the state machine forces it to sleep
	go func() {
		defer wg.Done()
		bounce.Bar()
	}()

	go func() {
		defer wg.Done()
		bounce.Foo()
	}()

	wg.Wait()
}
