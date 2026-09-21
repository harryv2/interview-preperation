package main

import "sync"

type Subscription struct {
	id    string
	topic *Topic

	mu     sync.Mutex
	offset int64
}

func newSubscription(id string, t *Topic, startOffset int64) *Subscription {
	return &Subscription{
		id:     id,
		topic:  t,
		offset: startOffset,
	}
}

func (s *Subscription) ID() string {
	return s.id
}

func (s *Subscription) Offset() int64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.offset
}

func (s *Subscription) Receive(limit int) []Message {
	return s.topic.read(s.Offset(), limit)
}

func (s *Subscription) Ack(id int64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.offset = max(s.offset, id)
}
