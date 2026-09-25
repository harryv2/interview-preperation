Event / Ticket Booking Platform

- Organiser lists an event, it runs as one or more occurrences at a venue on a date
- A venue is made of sections, a section is either reserved seating or standing
- Each tier sells one section, a buyer picks seats in a seated tier and a count in a standing tier
- One order can span tiers, and lands whole or not at all
- Selected tickets are held for a short window, an unpaid hold goes back on sale
- A declined payment keeps the hold, the buyer retries inside the window
- A retried payment with the same key does not buy the tickets twice
- A person can hold at most N tickets for one event

Entities

Money, Clock
Venue, Section, Seat
Event, TicketTier
Inventory (SeatedInventory, StandingInventory)
Occurrence
Order, OrderLine, TicketRef
Payment

Order status

HELD -> CONFIRMED
HELD -> EXPIRED (window ran out)
HELD -> CANCELLED (buyer walked away)

Decisions worth defending

- Two inventory kinds behind one interface, a concert floor sells a count against a capacity and a stand sells
  named seats, and a design that only knows seats cannot sell the floor at all
- Inventory is per tier per occurrence, never on the venue, the venue is a physical fact and the inventory is
  a fact about one night
- Occurrence owns the lock, because the all or nothing rule spans tiers, a per seat lock cannot express it
- Every seat is checked before any seat is taken, a partial grab is worse than a clean refusal
- Rollback on a mixed order happens under the same lock, so a half taken order is never visible to anyone
- Holds expire lazily on read and are reclaimed by a sweeper, a read must never show a ticket that is already gone
- Seat release and confirm check whose hold it is, because an expired hold can already have been overwritten
- The price is frozen on the order before the hold, otherwise under demand pricing a buyer pays for their own demand
- A declined payment does not release the hold, losing your seats because a card bounced is the wrong behaviour
- Clock is injected, a hold window is untestable if the test has to sleep through it

Swappable
- pricing (flat per tier, demand based on how much of the tier has gone)
- payment method, and its declined and unavailable outcomes are different things to the caller

Not modelled
- waitlists, seat maps and adjacency, dynamic seat suggestions, refunds and partial cancellation, resale,
  organiser payouts, promo codes, multi currency, distributed locks for more than one app server
