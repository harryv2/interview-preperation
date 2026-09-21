package main

import (
	"fmt"
	"sync"
)

func main() {
	broker := NewBroker()

	inventory, _ := broker.Subscribe("orders", "inventory")
	email, _ := broker.Subscribe("orders", "email")

	for i := range 5 {
		broker.Publish("orders", fmt.Sprintf("order-%d placed", i))
	}

	var wg sync.WaitGroup
	wg.Go(func() {
		consume(inventory, 2)
	})
	wg.Go(func() {
		consume(email, 10)
	})
	wg.Wait()

	audit, _ := broker.SubscribeFromStart("orders", "audit")
	consume(audit, 3)
}

func consume(sub *Subscription, batch int) {
	for {
		msgs := sub.Receive(batch)
		if len(msgs) == 0 {
			return
		}
		for _, m := range msgs {
			fmt.Println(sub.ID(), m.ID, m.Payload)
			sub.Ack(m.ID)
		}
	}
}
