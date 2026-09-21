package main

import (
	"fmt"
	"slices"
	"sync"
	"time"
)

type Topic struct {
	name string

	mu       sync.RWMutex
	messages []Message
	subs     map[string]*Subscription
}

func newTopic(name string) *Topic {
	return &Topic{
		name: name,
		subs: make(map[string]*Subscription),
	}
}

func (t *Topic) publish(payload string) Message {
	t.mu.Lock()
	defer t.mu.Unlock()

	msg := Message{
		ID:        int64(len(t.messages)) + 1,
		Payload:   payload,
		Timestamp: time.Now(),
	}
	t.messages = append(t.messages, msg)
	return msg
}

func (t *Topic) read(offset int64, limit int) []Message {
	t.mu.RLock()
	defer t.mu.RUnlock()

	end := min(offset+int64(limit), int64(len(t.messages)))
	if offset >= end {
		return nil
	}
	return slices.Clone(t.messages[offset:end])
}

func (t *Topic) subscribe(id string, fromStart bool) (*Subscription, error) {
	t.mu.Lock()
	defer t.mu.Unlock()

	if _, exists := t.subs[id]; exists {
		return nil, fmt.Errorf("subscriber %q already exists on topic %q", id, t.name)
	}
	start := int64(len(t.messages))
	if fromStart {
		start = 0
	}
	sub := newSubscription(id, t, start)
	t.subs[id] = sub
	return sub, nil
}

func (t *Topic) unsubscribe(id string) {
	t.mu.Lock()
	defer t.mu.Unlock()
	delete(t.subs, id)
}
