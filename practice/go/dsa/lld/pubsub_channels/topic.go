package main

import (
	"fmt"
	"maps"
	"slices"
	"sync"
)

type Topic struct {
	name string
	cfg  Config

	mu   sync.RWMutex
	subs map[string]*Subscription
}

func newTopic(name string, cfg Config) *Topic {
	return &Topic{name: name, cfg: cfg, subs: make(map[string]*Subscription)}
}

// publish holds the read lock while sending so a subscriber cannot be
// closed by unsubscribe mid-send
func (t *Topic) publish(msg Message) {
	t.mu.RLock()
	defer t.mu.RUnlock()

	for _, s := range t.subs {
		s.ch <- msg
	}
}

func (t *Topic) subscribe(id string, h Handler) error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if _, exists := t.subs[id]; exists {
		return fmt.Errorf("subscriber %q already exists on topic %q", id, t.name)
	}
	t.subs[id] = newSubscription(id, h, t.cfg)
	return nil
}

func (t *Topic) unsubscribe(id string) {
	t.mu.Lock()
	s, ok := t.subs[id]
	delete(t.subs, id)
	t.mu.Unlock()

	if ok {
		s.stop()
	}
}

func (t *Topic) deadLetters(id string) []DeadLetter {
	t.mu.RLock()
	s, ok := t.subs[id]
	t.mu.RUnlock()

	if !ok {
		return nil
	}
	return s.deadLetters()
}

func (t *Topic) close() {
	t.mu.RLock()
	subs := slices.Collect(maps.Values(t.subs))
	t.mu.RUnlock()

	for _, s := range subs {
		s.stop()
	}
}
