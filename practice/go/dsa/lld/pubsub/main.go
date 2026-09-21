package main

import (
	"errors"
	"fmt"
	"time"
)

func main() {
	broker := NewBroker(Config{MaxAttempts: 2, RetryDelay: 10 * time.Millisecond})

	broker.Subscribe("orders", "inventory", func(m Message) error {
		fmt.Println("inventory:", m.Payload)
		return nil
	})
	broker.Subscribe("orders", "email", func(m Message) error {
		fmt.Println("email:", m.Payload)
		return nil
	})
	broker.Subscribe("orders", "flaky", func(m Message) error {
		return errors.New("cannot process " + m.Payload)
	})

	broker.Publish("orders", "order-123 placed")

	broker.SubscribeFromStart("orders", "audit", func(m Message) error {
		fmt.Println("audit:", m.Payload)
		return nil
	})

	broker.Close()
	for _, dl := range broker.DeadLetters("orders", "flaky") {
		fmt.Println("dead letter:", dl.Message.Payload, "->", dl.Err)
	}
}
