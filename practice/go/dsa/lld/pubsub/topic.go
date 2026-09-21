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

	mu       sync.RWMutex
	messages []Message
	subs     map[string]*Subscription
}

func newTopic(name string, cfg Config) *Topic {
	return &Topic{name: name, cfg: cfg, subs: make(map[string]*Subscription)}
}

func (t *Topic) publish(msg Message) {
	t.mu.Lock()
	defer t.mu.Unlock()

	t.messages = append(t.messages, msg)
	for _, s := range t.subs {
		s.signal()
	}
}

func (t *Topic) messageAt(offset int) (Message, bool) {
	t.mu.RLock()
	defer t.mu.RUnlock()

	if offset >= len(t.messages) {
		return Message{}, false
	}
	return t.messages[offset], true
}

func (t *Topic) subscribe(id string, h Handler, fromStart bool) error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if _, exists := t.subs[id]; exists {
		return fmt.Errorf("subscriber %q already exists on topic %q", id, t.name)
	}
	offset := len(t.messages)
	if fromStart {
		offset = 0
	}
	t.subs[id] = newSubscription(id, h, t, offset)
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
