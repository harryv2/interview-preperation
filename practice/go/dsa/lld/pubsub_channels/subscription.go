package main

import (
	"sync"
	"time"
)

type Subscription struct {
	id      string
	handler Handler
	cfg     Config
	ch      chan Message
	once    sync.Once
	wg      sync.WaitGroup

	dlqMu sync.Mutex
	dlq   []DeadLetter
}

func newSubscription(id string, h Handler, cfg Config) *Subscription {
	s := &Subscription{
		id:      id,
		handler: h,
		cfg:     cfg,
		ch:      make(chan Message, cfg.Buffer),
	}
	s.wg.Go(s.run)
	return s
}

func (s *Subscription) run() {
	for msg := range s.ch {
		s.deliver(msg)
	}
}

func (s *Subscription) deliver(msg Message) {
	for attempt := 1; ; attempt++ {
		err := s.handler(msg)
		if err == nil {
			return
		}
		if attempt >= s.cfg.MaxAttempts {
			s.deadLetter(msg, err)
			return
		}
		time.Sleep(s.cfg.RetryDelay)
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
		close(s.ch)
	})
	s.wg.Wait()
}
