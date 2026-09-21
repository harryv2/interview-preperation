package main

import "sync"

type Broker struct {
	mu     sync.RWMutex
	topics map[string]*Topic
}

func NewBroker() *Broker {
	return &Broker{
		topics: make(map[string]*Topic),
	}
}

func (b *Broker) lookup(name string) (*Topic, bool) {
	b.mu.RLock()
	defer b.mu.RUnlock()
	t, ok := b.topics[name]
	return t, ok
}

func (b *Broker) getOrCreate(name string) *Topic {
	if t, ok := b.lookup(name); ok {
		return t
	}

	b.mu.Lock()
	defer b.mu.Unlock()
	if t, ok := b.topics[name]; ok {
		return t
	}
	t := newTopic(name)
	b.topics[name] = t
	return t
}

func (b *Broker) Publish(topic, payload string) int64 {
	return b.getOrCreate(topic).publish(payload).ID
}

func (b *Broker) Subscribe(topic, id string) (*Subscription, error) {
	return b.getOrCreate(topic).subscribe(id, false)
}

func (b *Broker) SubscribeFromStart(topic, id string) (*Subscription, error) {
	return b.getOrCreate(topic).subscribe(id, true)
}

func (b *Broker) Unsubscribe(topic, id string) {
	if t, ok := b.lookup(topic); ok {
		t.unsubscribe(id)
	}
}
