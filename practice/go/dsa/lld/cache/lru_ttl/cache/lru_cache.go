package cache

import (
	"sync"
	"time"

	"github.com/harryv2/interview-preperation/practice/go/dsa/lld/cache/lru_ttl/doubly_list"
)

type cacheNode[U any] struct {
	val        U
	expiryTime time.Time
}

type Clock interface {
	Now() time.Time
}

type SystemClock struct{}

func (SystemClock) Now() time.Time {
	return time.Now()
}

type LruCache[T comparable, U any] struct {
	maxSize   int
	cacheMap  map[T]*doubly_list.DoublyLinkedListNode[T, cacheNode[U]]
	list      *doubly_list.DoublyLinkedList[T, cacheNode[U]]
	clock     Clock
	done      chan struct{}
	closeOnce sync.Once

	mu sync.RWMutex
}

func NewLruCache[T comparable, U any](maxSize int, clock Clock) *LruCache[T, U] {
	cache := &LruCache[T, U]{
		maxSize:  maxSize,
		cacheMap: make(map[T]*doubly_list.DoublyLinkedListNode[T, cacheNode[U]]),
		list:     doubly_list.NewDoublyLinkedList[T, cacheNode[U]](),
		clock:    clock,
		done:     make(chan struct{}),
	}

	go cache.runSweeper()

	return cache
}

func (cache *LruCache[T, U]) Size() int {
	cache.mu.RLock()
	defer cache.mu.RUnlock()

	return len(cache.cacheMap)
}

func (cache *LruCache[T, U]) Contains(key T) bool {
	cache.mu.Lock()
	defer cache.mu.Unlock()

	node, ok := cache.cacheMap[key]
	if !ok {
		return false
	}

	if node.Value.expiryTime.Before(cache.clock.Now()) {
		cache.delete(node)
		return false
	}

	return true
}

func (cache *LruCache[T, U]) evict() {
	first, err := cache.list.RemoveFirst()
	if err != nil {
		return
	}

	delete(cache.cacheMap, first.Key)
}

func (cache *LruCache[T, U]) Add(key T, value U, ttl time.Duration) {
	cache.mu.Lock()
	defer cache.mu.Unlock()

	node, ok := cache.cacheMap[key]
	if ok {
		node.Value.val = value
		node.Value.expiryTime = cache.clock.Now().Add(ttl)
		cache.list.Remove(node)
		cache.list.AddNodeLast(node)
		return
	}

	if len(cache.cacheMap) >= cache.maxSize {
		cache.evict()
	}

	node = &doubly_list.DoublyLinkedListNode[T, cacheNode[U]]{
		Key: key,
		Value: cacheNode[U]{
			val:        value,
			expiryTime: cache.clock.Now().Add(ttl),
		},
	}
	cache.cacheMap[key] = node
	cache.list.AddNodeLast(node)
}

func (cache *LruCache[T, U]) delete(node *doubly_list.DoublyLinkedListNode[T, cacheNode[U]]) {
	cache.list.Remove(node)
	delete(cache.cacheMap, node.Key)
}

func (cache *LruCache[T, U]) Get(key T) (U, bool) {
	cache.mu.Lock()
	defer cache.mu.Unlock()

	var zero U

	node, ok := cache.cacheMap[key]
	if !ok {
		return zero, false
	}

	now := cache.clock.Now()

	if node.Value.expiryTime.Before(now) {
		cache.delete(node)
		return zero, false
	}

	cache.list.Remove(node)
	cache.list.AddNodeLast(node)

	return node.Value.val, true
}

func (cache *LruCache[T, U]) sweep() {
	cache.mu.Lock()
	defer cache.mu.Unlock()

	now := cache.clock.Now()

	var expired []*doubly_list.DoublyLinkedListNode[T, cacheNode[U]]
	for _, node := range cache.cacheMap {
		if node.Value.expiryTime.Before(now) {
			expired = append(expired, node)
		}
	}

	for _, node := range expired {
		cache.delete(node)
	}
}

func (cache *LruCache[T, U]) runSweeper() {
	ticker := time.NewTicker(2 * time.Second)

	defer ticker.Stop()

	for {
		select {
		case <-cache.done:
			return
		case <-ticker.C:
			cache.sweep()
		}
	}
}

func (cache *LruCache[T, U]) Close() {
	cache.closeOnce.Do(func() {
		close(cache.done)
	})
}
