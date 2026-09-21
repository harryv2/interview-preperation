package main

import (
	"sync"
	"time"
)

type Subscription struct {
	id      string
	handler Handler
	topic   *Topic
	offset  int
	wake    chan struct{}
	once    sync.Once
	wg      sync.WaitGroup

	dlqMu sync.Mutex
	dlq   []DeadLetter
}

func newSubscription(id string, h Handler, t *Topic, offset int) *Subscription {
	s := &Subscription{
		id:      id,
		handler: h,
		topic:   t,
		offset:  offset,
		wake:    make(chan struct{}, 1),
	}
	s.wg.Go(s.run)
	s.signal()
	return s
}

func (s *Subscription) signal() {
	select {
	case s.wake <- struct{}{}:
	default:
	}
}

func (s *Subscription) run() {
	for range s.wake {
		s.drain()
	}
}

func (s *Subscription) drain() {
	for {
		msg, ok := s.topic.messageAt(s.offset)
		if !ok {
			return
		}
		s.deliver(msg)
		s.offset++
	}
}

func (s *Subscription) deliver(msg Message) {
	cfg := s.topic.cfg
	for attempt := 1; ; attempt++ {
		err := s.handler(msg)
		if err == nil {
			return
		}
		if attempt >= cfg.MaxAttempts {
			s.deadLetter(msg, err)
			return
		}
		time.Sleep(cfg.RetryDelay)
	}
}

func (s *Subscription) deadLetter(msg Message, err error) {
	s.dlqMu.Lock()
	defer s.dlqMu.Unlock()
	s.dlq = append(s.dlq, DeadLetter{msg, err})
}

func (s *Subscription) deadLetters() []DeadLetter {
	s.dlqMu.Lock()
	defer s.dlqMu.Unlock()
	return append([]DeadLetter(nil), s.dlq...)
}

func (s *Subscription) stop() {
	s.once.Do(func() {
		close(s.wake)
	})
	s.wg.Wait()
}
