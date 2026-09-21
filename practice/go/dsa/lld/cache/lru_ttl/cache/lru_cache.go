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
	now() time.Time
}

type SystemClock struct {
}

func (c *SystemClock) now() time.Time {
	return time.Now()
}

type LruCache[T comparable, U any] struct {
	MaxSize  int
	cacheMap map[T]*doubly_list.DoublyLinkedListNode[T, cacheNode[U]]
	list     *doubly_list.DoublyLinkedList[T, cacheNode[U]]
	clock    Clock

	mu sync.RWMutex
}

func NewLruCache[T comparable, U any](maxSize int, clock Clock) *LruCache[T, U] {
	return &LruCache[T, U]{
		MaxSize:  maxSize,
		cacheMap: make(map[T]*doubly_list.DoublyLinkedListNode[T, cacheNode[U]]),
		list:     doubly_list.NewDoublyLinkedList[T, cacheNode[U]](),
		clock:    clock,
	}
}

func (cache *LruCache[T, U]) Size() int {
	cache.mu.RLock()
	defer cache.mu.RUnlock()

	return len(cache.cacheMap)
}

func (cache *LruCache[T, U]) Contains(key T) bool {
	cache.mu.RLock()
	defer cache.mu.RUnlock()

	_, ok := cache.cacheMap[key]
	return ok
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
		node.Value.expiryTime = cache.clock.now().Add(ttl)
		cache.list.Remove(node)
		cache.list.AddNodeLast(node)
		return
	}

	if len(cache.cacheMap) >= cache.MaxSize {
		cache.evict()
	}

	node = &doubly_list.DoublyLinkedListNode[T, cacheNode[U]]{
		Key: key,
		Value: cacheNode[U]{
			val:        value,
			expiryTime: cache.clock.now().Add(ttl),
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

	now := cache.clock.now()

	if node.Value.expiryTime.Before(now) {
		cache.delete(node)
	}

	cache.list.Remove(node)
	cache.list.AddNodeLast(node)

	return node.Value.val, true
}
