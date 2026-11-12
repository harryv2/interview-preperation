package main

import (
	"fmt"
	"sync"
)

type H2O struct {
	mu   sync.Mutex
	cond *sync.Cond
	h    int
	o    int
}

func NewH2O() *H2O {
	m := &H2O{}
	m.cond = sync.NewCond(&m.mu)
	return m
}

func (m *H2O) Hydrogen(releaseHydrogen func()) {
	m.mu.Lock()
	defer m.mu.Unlock()

	// 1. Block if we already have 2 Hydrogens waiting to form a molecule
	for m.h == 2 {
		m.cond.Wait()
	}

	// 2. Claim a Hydrogen spot
	m.h++
	releaseHydrogen()

	// 3. If the molecule is complete, print a separator and reset the state
	if m.h == 2 && m.o == 1 {
		fmt.Print(" ")
		m.h, m.o = 0, 0
	}

	// 4. Wake up any sleeping threads (maybe an Oxygen thread was waiting)
	m.cond.Broadcast()
}

func (m *H2O) Oxygen(releaseOxygen func()) {
	m.mu.Lock()
	defer m.mu.Unlock()

	// 1. Block if we already have 1 Oxygen waiting to form a molecule
	for m.o == 1 {
		m.cond.Wait()
	}

	// 2. Claim the Oxygen spot
	m.o++
	releaseOxygen()

	// 3. If the molecule is complete, print a separator and reset the state
	if m.h == 2 && m.o == 1 {
		fmt.Print(" ")
		m.h, m.o = 0, 0
	}

	// 4. Wake up any sleeping threads (maybe Hydrogen threads were waiting)
	m.cond.Broadcast()
}

func main() {
	h2o := NewH2O()
	var wg sync.WaitGroup

	// A balanced input string (12 Hydrogens, 6 Oxygens = 6 Water Molecules)
	// In a real interview, they might pass this as an argument.
	input := "HOHOHHHOHOHHHOHOHH"

	releaseHydrogen := func() {
		fmt.Print("H") // Output might look like HHO, HOH, or OHH
	}

	releaseOxygen := func() {
		fmt.Print("O")
	}

	// Fan-out: Spawn a goroutine for every single atom
	for _, char := range input {
		wg.Add(1) // RULE: Always Add(1) BEFORE launching the goroutine

		if char == 'H' {
			go func() {
				defer wg.Done()
				h2o.Hydrogen(releaseHydrogen)
			}()
		} else if char == 'O' {
			go func() {
				defer wg.Done()
				h2o.Oxygen(releaseOxygen)
			}()
		}
	}

	// Fan-in: Wait for all threads to complete their molecule binding
	wg.Wait()
	fmt.Println("\nAll molecules formed successfully.")
}
