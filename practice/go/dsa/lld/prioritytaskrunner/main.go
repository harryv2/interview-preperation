package main

import (
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
	"time"
)

func main() {
	fmt.Println("-- 1 worker: strict priority order once the worker is free")
	var mu sync.Mutex
	var order []string
	record := func(name string) {
		mu.Lock()
		order = append(order, name)
		mu.Unlock()
	}

	r := NewRunner(1, 10*time.Second)
	Submit(r, "blocker", Low, func() (struct{}, error) {
		time.Sleep(50 * time.Millisecond)
		return struct{}{}, nil
	})
	jobs := []struct {
		name     string
		priority Priority
	}{
		{"low1", Low}, {"med1", Medium}, {"high1", High}, {"high2", High}, {"med2", Medium},
	}
	for _, j := range jobs {
		name := j.name
		Submit(r, name, j.priority, func() (struct{}, error) {
			record(name)
			return struct{}{}, nil
		})
	}
	r.Close()
	fmt.Println(" ", order)

	fmt.Println("-- aging: a LOW task that waited past maxWait jumps the HIGH queue")
	order = nil
	r = NewRunner(1, 100*time.Millisecond)
	Submit(r, "blocker", High, func() (struct{}, error) {
		time.Sleep(60 * time.Millisecond)
		return struct{}{}, nil
	})
	Submit(r, "low", Low, func() (struct{}, error) {
		record("low")
		return struct{}{}, nil
	})
	for i := 1; i <= 5; i++ {
		name := fmt.Sprintf("high%d", i)
		Submit(r, name, High, func() (struct{}, error) {
			record(name)
			time.Sleep(30 * time.Millisecond)
			return struct{}{}, nil
		})
	}
	r.Close()
	fmt.Println(" ", order)

	fmt.Println("-- at most K run at once")
	var running, peak atomic.Int32
	r = NewRunner(3, time.Second)
	var futures []*Future[int]
	for i := 1; i <= 12; i++ {
		n := i
		f, _ := Submit(r, fmt.Sprintf("job%d", n), Priority(n%3), func() (int, error) {
			now := running.Add(1)
			for {
				old := peak.Load()
				if now <= old || peak.CompareAndSwap(old, now) {
					break
				}
			}
			time.Sleep(20 * time.Millisecond)
			running.Add(-1)
			return n * n, nil
		})
		futures = append(futures, f)
	}
	var results []int
	for _, f := range futures {
		v, _ := f.Wait()
		results = append(results, v)
	}
	r.Close()
	fmt.Println("  results =", results)
	fmt.Printf("  peak concurrency = %d (limit 3)\n", peak.Load())

	fmt.Println("-- errors and panics surface on the future")
	r = NewRunner(1, time.Second)
	failed, _ := Submit(r, "fails", High, func() (int, error) {
		return 0, errors.New("boom")
	})
	panicked, _ := Submit(r, "panics", High, func() (int, error) {
		panic("kaboom")
	})
	_, err := failed.Wait()
	fmt.Println(" ", err)
	_, err = panicked.Wait()
	fmt.Println(" ", err)
	r.Close()

	fmt.Println("-- submit after close is rejected")
	_, err = Submit(r, "late", Low, func() (int, error) { return 1, nil })
	fmt.Println(" ", err)
}
