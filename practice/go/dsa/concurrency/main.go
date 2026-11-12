package main

import (
	"fmt"
	"sync"
)

// Define constants for the state machine to make the code highly readable
const (
	TurnZero = 0
	TurnOdd  = 1
	TurnEven = 2
)

type ZeroEvenOdd struct {
	n    int
	mu   sync.Mutex
	cond *sync.Cond
	turn int // 0 = Zero, 1 = Odd, 2 = Even
}

func NewZeroEvenOdd(n int) *ZeroEvenOdd {
	z := &ZeroEvenOdd{
		n:    n,
		turn: TurnZero, // Zero always starts
	}
	z.cond = sync.NewCond(&z.mu)
	return z
}

func (z *ZeroEvenOdd) Zero() {
	// Loop from 1 to n (e.g., 1, 2, 3, 4, 5)
	for i := 1; i <= z.n; i++ {
		z.mu.Lock()

		// 1. Wait if it is NOT Zero's turn
		for z.turn != TurnZero {
			z.cond.Wait()
		}

		// 2. Execute
		fmt.Print("0")

		// 3. Bounce control based on the current step
		if i%2 == 1 {
			z.turn = TurnOdd // If 'i' is odd, let the Odd thread go
		} else {
			z.turn = TurnEven // If 'i' is even, let the Even thread go
		}

		// 4. Wake up ALL threads
		z.cond.Broadcast()

		z.mu.Unlock()
	}
}

func (z *ZeroEvenOdd) Odd() {
	for i := 1; i <= z.n; i += 2 {
		z.mu.Lock()

		// 1. Wait if it is NOT Odd's turn
		for z.turn != TurnOdd {
			z.cond.Wait()
		}

		// 2. Execute
		fmt.Print(i)

		// 3. Bounce control back to Zero
		z.turn = TurnZero

		// 4. Wake up ALL threads
		z.cond.Broadcast()

		z.mu.Unlock()
	}
}

func (z *ZeroEvenOdd) Even() {
	for i := 2; i <= z.n; i += 2 {
		z.mu.Lock()

		// 1. Wait if it is NOT Even's turn
		for z.turn != TurnEven {
			z.cond.Wait()
		}

		// 2. Execute
		fmt.Print(i)

		// 3. Bounce control back to Zero
		z.turn = TurnZero

		// 4. Wake up ALL threads
		z.cond.Broadcast()

		z.mu.Unlock()
	}
}

func main() {
	n := 5 // Expected output: 0102030405
	z := NewZeroEvenOdd(n)
	var wg sync.WaitGroup

	wg.Add(3)

	go func() { defer wg.Done(); z.Zero() }()
	go func() { defer wg.Done(); z.Even() }()
	go func() { defer wg.Done(); z.Odd() }()

	wg.Wait()
	fmt.Println()
}
