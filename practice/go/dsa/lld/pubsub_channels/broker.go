package main

import (
	"errors"
	"maps"
	"slices"
	"sync"

	"github.com/google/uuid"
)

var ErrClosed = errors.New("broker closed")

type Broker struct {
	cfg    Config
	mu     sync.Mutex
	topics map[string]*Topic
	closed bool
}

func NewBroker(cfg Config) *Broker {
	return &Broker{cfg: cfg, topics: make(map[string]*Topic)}
}

func (b *Broker) topic(name string) (*Topic, error) {
	b.mu.Lock()
	defer b.mu.Unlock()

	if b.closed {
		return nil, ErrClosed
	}
	t, ok := b.topics[name]
	if !ok {
		t = newTopic(name, b.cfg)
		b.topics[name] = t
	}
	return t, nil
}

func (b *Broker) lookup(name string) *Topic {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.topics[name]
}

func (b *Broker) Publish(topic, payload string) error {
	t, err := b.topic(topic)
	if err != nil {
		return err
	}
	t.publish(Message{ID: uuid.NewString(), Payload: payload})
	return nil
}

func (b *Broker) Subscribe(topic, id string, h Handler) error {
	t, err := b.topic(topic)
	if err != nil {
		return err
	}
	return t.subscribe(id, h)
}

func (b *Broker) Unsubscribe(topic, id string) {
	if t := b.lookup(topic); t != nil {
		t.unsubscribe(id)
	}
}

func (b *Broker) DeadLetters(topic, id string) []DeadLetter {
	if t := b.lookup(topic); t != nil {
		return t.deadLetters(id)
	}
	return nil
}

func (b *Broker) Close() {
	b.mu.Lock()
	b.closed = true
	topics := slices.Collect(maps.Values(b.topics))
	b.mu.Unlock()

	for _, t := range topics {
		t.close()
	}
}
