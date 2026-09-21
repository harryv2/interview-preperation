package main

import "time"

type Message struct {
	ID        int64
	Payload   string
	Timestamp time.Time
}
