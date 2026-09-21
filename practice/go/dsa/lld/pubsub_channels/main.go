package main

import (
	"errors"
	"fmt"
	"time"
)

func main() {
	broker := NewBroker(Config{Buffer: 64, MaxAttempts: 2, RetryDelay: 10 * time.Millisecond})

	_ = broker.Subscribe("orders", "inventory", func(m Message) error {
		fmt.Println("inventory:", m.Payload)
		return nil
	})
	_ = broker.Subscribe("orders", "email", func(m Message) error {
		fmt.Println("email:", m.Payload)
		return nil
	})
	_ = broker.Subscribe("orders", "flaky", func(m Message) error {
		return errors.New("cannot process " + m.Payload)
	})

	_ = broker.Publish("orders", "order-123 placed")

	broker.Close()
	for _, dl := range broker.DeadLetters("orders", "flaky") {
		fmt.Println("dead letter:", dl.Message.Payload, "->", dl.Err)
	}
}
