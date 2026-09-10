package main

import (
	"fmt"
	"sync"
	"time"
)

//Problem 2 — Design a Concurrent Token Rate Limiter
//
//Problem
//Build an in-memory, thread-safe rate limiter that evaluates requests based on variable token weight (e.g., LLM context window size) rather than unit request counts.
//
//Implement the TokenRateLimiter struct/class:
//allow(clientID, tokenWeight, nowSec): Return true if the request is permitted within the sliding window, or false if it exceeds the max allowed tokens.
//
//Functional Constraints
//Client Scale: Up to 10^6 unique active client IDs.
//Token Range: 1 <= tokenWeight <= 10^5 per request.
//Window Size: Bounded time window W (e.g., 60 seconds). Max capacity C tokens.
//
//Performance Constraints
//Time Complexity: Strict O(1) amortized time complexity for allow.
//Space Complexity: O(U) where U is the number of active clients.
//Concurrency: Must support 100k+ QPS across concurrent worker threads without a single global mutex lock bottleneck.

type Window map[int64]int64

type ClientID string

type RateLimiter struct {
	userCountMap map[ClientID]Window
	userLockMap  map[ClientID]*sync.Mutex
	globalLock   sync.Mutex
	window       time.Duration
	maxCapacity  int64
}

func NewRateLimiter(window time.Duration, maxCapacity int64) *RateLimiter {
	return &RateLimiter{
		userCountMap: make(map[ClientID]Window),
		userLockMap:  make(map[ClientID]*sync.Mutex),
		window:       window,
		maxCapacity:  maxCapacity,
	}
}

func (limiter *RateLimiter) getUserLock(userId ClientID) *sync.Mutex {
	limiter.globalLock.Lock()
	defer limiter.globalLock.Unlock()

	val, ok := limiter.userLockMap[userId]
	if ok {
		return val
	}

	limiter.userLockMap[userId] = &sync.Mutex{}
	return limiter.userLockMap[userId]
}

func (limiter *RateLimiter) getCountForUserInWindow(id ClientID, now time.Time) int64 {
	userMap, ok := limiter.userCountMap[id]
	if !ok {
		return 0
	}

	window := limiter.window
	pastTime := now.Add(-window)
	sum := int64(0)

	for i := pastTime.Unix(); i <= now.Unix(); i++ {
		val, ok := userMap[i]
		if ok {
			sum += val
		}
	}

	return sum
}

func (limiter *RateLimiter) addToUserMap(id ClientID, tokenWeight int64, now time.Time) {
	userMap, ok := limiter.userCountMap[id]
	if !ok {
		limiter.userCountMap[id] = make(Window)
	}

	userMap = limiter.userCountMap[id]
	userMap[now.Unix()] += tokenWeight
}

func (limiter *RateLimiter) Allow(id ClientID, tokenWeight int64, now time.Time) bool {
	lock := limiter.getUserLock(id)
	lock.Lock()
	defer lock.Unlock()

	totalWindowCount := limiter.getCountForUserInWindow(id, now)
	if (totalWindowCount + tokenWeight) > limiter.maxCapacity {
		return false
	}
	limiter.addToUserMap(id, tokenWeight, now)
	return true
}

func main() {

	limiter := NewRateLimiter(time.Second*10, 100)

	g := sync.WaitGroup{}

	var id ClientID = "abc"

	now := time.Now()

	for i := 0; i < 10; i++ {
		g.Add(1)
		go func() {
			fmt.Println(i, ":", limiter.Allow(id, 20, now))
			g.Done()
		}()
	}

	g.Wait()
}
