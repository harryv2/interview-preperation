package pubsub

import (
	"fmt"
	"log"
	"sync"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
)

// ---------- Message ----------

type Message struct {
	ID        string
	Payload   string
	Timestamp time.Time
}

// Handler processes a message. Returning an error triggers retry.
type Handler func(Message) error

// ---------- Config ----------

type Config struct {
	MaxRetries     int
	InitialBackoff time.Duration
	Workers        int // 0 = unlimited goroutines
}

func DefaultConfig() Config {
	return Config{
		MaxRetries:     3,
		InitialBackoff: 100 * time.Millisecond,
	}
}

// ---------- Subscription ----------

type Subscription struct {
	id      string
	handler Handler
	topic   *Topic
	cfg     Config

	offset int64 // next message index to read; atomic

	wake chan struct{} // capacity 1: "work pending"
	done chan struct{}
	wg   sync.WaitGroup
	once sync.Once
}

func newSubscription(id string, h Handler, t *Topic, cfg Config, startOffset int64) *Subscription {
	s := &Subscription{
		id:      id,
		handler: h,
		topic:   t,
		cfg:     cfg,
		offset:  startOffset,
		wake:    make(chan struct{}, 1),
		done:    make(chan struct{}),
	}
	s.wg.Add(1)
	go s.run()
	s.signal() // drain anything already in the log
	return s
}

// signal marks work as pending. Never blocks.
func (s *Subscription) signal() {
	select {
	case s.wake <- struct{}{}:
	default: // a wake-up is already queued
	}
}

func (s *Subscription) run() {
	defer s.wg.Done()
	for {
		select {
		case <-s.wake:
			s.drain()
		case <-s.done:
			return
		}
	}
}

// drain reads forward from the current offset until caught up.
func (s *Subscription) drain() {
	for {
		select {
		case <-s.done:
			return
		default:
		}

		msg, ok := s.topic.messageAt(atomic.LoadInt64(&s.offset))
		if !ok {
			return // caught up
		}
		if !s.deliver(msg) {
			return // shutting down mid-delivery
		}
		atomic.AddInt64(&s.offset, 1)
	}
}

// deliver runs the handler with retry. Returns false only on shutdown.
func (s *Subscription) deliver(msg Message) bool {
	backoff := s.cfg.InitialBackoff

	for attempt := 1; attempt <= s.cfg.MaxRetries; attempt++ {
		err := s.safeCall(msg)
		if err == nil {
			return true
		}
		if attempt == s.cfg.MaxRetries {
			s.topic.deadLetter(s.id, msg, err)
			return true // skip poison message, keep the subscription alive
		}
		select {
		case <-time.After(backoff):
			backoff *= 2
		case <-s.done:
			return false
		}
	}
	return true
}

// safeCall converts a panicking handler into an error.
func (s *Subscription) safeCall(msg Message) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("handler panic: %v", r)
		}
	}()
	return s.handler(msg)
}

func (s *Subscription) stop() {
	s.once.Do(func() { close(s.done) })
	s.wg.Wait()
}

func (s *Subscription) Offset() int64 { return atomic.LoadInt64(&s.offset) }

// ---------- Topic ----------

type Topic struct {
	name string
	cfg  Config

	mu       sync.RWMutex
	messages []Message
	base     int64 // log index of messages[0], grows as we trim
	subs     map[string]*Subscription

	dlqMu sync.Mutex
	dlq   []DeadLetter
}

type DeadLetter struct {
	SubscriberID string
	Message      Message
	Err          string
	FailedAt     time.Time
}

func newTopic(name string, cfg Config) *Topic {
	return &Topic{
		name: name,
		cfg:  cfg,
		subs: make(map[string]*Subscription),
	}
}

func (t *Topic) publish(msg Message) int64 {
	t.mu.Lock()
	t.messages = append(t.messages, msg)
	index := t.base + int64(len(t.messages)) - 1
	t.mu.Unlock()

	t.mu.RLock()
	for _, s := range t.subs {
		s.signal()
	}
	t.mu.RUnlock()

	return index
}

// messageAt returns the message at a log index.
func (t *Topic) messageAt(offset int64) (Message, bool) {
	t.mu.RLock()
	defer t.mu.RUnlock()

	pos := offset - t.base
	if pos < 0 {
		// offset was trimmed away; skip ahead to the oldest we still hold
		if len(t.messages) == 0 {
			return Message{}, false
		}
		return t.messages[0], true
	}
	if pos >= int64(len(t.messages)) {
		return Message{}, false
	}
	return t.messages[pos], true
}

func (t *Topic) size() int64 {
	t.mu.RLock()
	defer t.mu.RUnlock()
	return t.base + int64(len(t.messages))
}

func (t *Topic) subscribe(id string, h Handler, fromStart bool) error {
	t.mu.Lock()
	if _, exists := t.subs[id]; exists {
		t.mu.Unlock()
		return fmt.Errorf("subscriber %q already exists on topic %q", id, t.name)
	}
	start := t.base + int64(len(t.messages)) // default: only new messages
	if fromStart {
		start = t.base
	}
	t.mu.Unlock()

	sub := newSubscription(id, h, t, t.cfg, start)

	t.mu.Lock()
	t.subs[id] = sub
	t.mu.Unlock()
	return nil
}

func (t *Topic) unsubscribe(id string) {
	t.mu.Lock()
	sub, ok := t.subs[id]
	delete(t.subs, id)
	t.mu.Unlock()

	if ok {
		sub.stop() // outside the lock: stop() waits on a goroutine
	}
}

// trim drops messages older than the slowest subscriber, keeping at most keep.
func (t *Topic) trim(keep int64) {
	t.mu.Lock()
	defer t.mu.Unlock()

	total := int64(len(t.messages))
	if total <= keep {
		return
	}

	slowest := t.base + total
	for _, s := range t.subs {
		if o := s.Offset(); o < slowest {
			slowest = o
		}
	}

	dropTo := t.base + total - keep
	if slowest < dropTo {
		dropTo = slowest // never drop unread messages
	}
	drop := dropTo - t.base
	if drop <= 0 {
		return
	}

	t.messages = append([]Message(nil), t.messages[drop:]...)
	t.base += drop
}

func (t *Topic) deadLetter(subID string, msg Message, err error) {
	t.dlqMu.Lock()
	t.dlq = append(t.dlq, DeadLetter{subID, msg, err.Error(), time.Now()})
	t.dlqMu.Unlock()
	log.Printf("DLQ topic=%s sub=%s msg=%s err=%v", t.name, subID, msg.ID, err)
}

func (t *Topic) deadLetters() []DeadLetter {
	t.dlqMu.Lock()
	defer t.dlqMu.Unlock()
	return append([]DeadLetter(nil), t.dlq...)
}

func (t *Topic) stopAll() {
	t.mu.Lock()
	subs := make([]*Subscription, 0, len(t.subs))
	for _, s := range t.subs {
		subs = append(subs, s)
	}
	t.subs = make(map[string]*Subscription)
	t.mu.Unlock()

	for _, s := range subs {
		s.stop()
	}
}

// ---------- Broker ----------

type Broker struct {
	cfg Config

	mu     sync.RWMutex
	topics map[string]*Topic
	closed bool
}

func NewBroker(cfg Config) *Broker {
	if cfg.MaxRetries <= 0 {
		cfg.MaxRetries = 3
	}
	if cfg.InitialBackoff <= 0 {
		cfg.InitialBackoff = 100 * time.Millisecond
	}
	return &Broker{cfg: cfg, topics: make(map[string]*Topic)}
}

func (b *Broker) getOrCreate(name string) (*Topic, error) {
	b.mu.RLock()
	if b.closed {
		b.mu.RUnlock()
		return nil, fmt.Errorf("broker closed")
	}
	t, ok := b.topics[name]
	b.mu.RUnlock()
	if ok {
		return t, nil
	}

	b.mu.Lock()
	defer b.mu.Unlock()
	if b.closed {
		return nil, fmt.Errorf("broker closed")
	}
	if t, ok := b.topics[name]; ok { // re-check under write lock
		return t, nil
	}
	t = newTopic(name, b.cfg)
	b.topics[name] = t
	return t, nil
}

func (b *Broker) Publish(topic, payload string) (int64, error) {
	t, err := b.getOrCreate(topic)
	if err != nil {
		return 0, err
	}
	return t.publish(Message{
		ID:        uuid.NewString(),
		Payload:   payload,
		Timestamp: time.Now(),
	}), nil
}

// Subscribe receives only messages published from now on.
func (b *Broker) Subscribe(topic, id string, h Handler) error {
	t, err := b.getOrCreate(topic)
	if err != nil {
		return err
	}
	return t.subscribe(id, h, false)
}

// SubscribeFromStart replays the whole retained log first.
func (b *Broker) SubscribeFromStart(topic, id string, h Handler) error {
	t, err := b.getOrCreate(topic)
	if err != nil {
		return err
	}
	return t.subscribe(id, h, true)
}

func (b *Broker) Unsubscribe(topic, id string) {
	b.mu.RLock()
	t, ok := b.topics[topic]
	b.mu.RUnlock()
	if ok {
		t.unsubscribe(id)
	}
}

func (b *Broker) Trim(topic string, keep int64) {
	b.mu.RLock()
	t, ok := b.topics[topic]
	b.mu.RUnlock()
	if ok {
		t.trim(keep)
	}
}

func (b *Broker) DeadLetters(topic string) []DeadLetter {
	b.mu.RLock()
	t, ok := b.topics[topic]
	b.mu.RUnlock()
	if !ok {
		return nil
	}
	return t.deadLetters()
}

func (b *Broker) Shutdown() {
	b.mu.Lock()
	if b.closed {
		b.mu.Unlock()
		return
	}
	b.closed = true
	topics := make([]*Topic, 0, len(b.topics))
	for _, t := range b.topics {
		topics = append(topics, t)
	}
	b.mu.Unlock()

	for _, t := range topics {
		t.stopAll()
	}
}
