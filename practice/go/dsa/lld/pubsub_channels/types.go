package main

import "time"

type Message struct {
	ID      string
	Payload string
}

type Handler func(Message) error

type DeadLetter struct {
	Message Message
	Err     error
}

type Config struct {
	Buffer      int
	MaxAttempts int
	RetryDelay  time.Duration
}
