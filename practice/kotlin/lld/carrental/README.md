Car Rental / Reservation System

- Customer searches cars of a type available at a branch for a date range
- Customer reserves one car for that range, price is quoted up front
- A car can not hold two overlapping reservations
- Concurrent reservations for the last free car must not double book
- Customer can cancel before pickup
- Pickup and dropoff move the reservation forward, dropoff charges late fees

Entities

Money
TimeRange
Car
Branch
Customer
Reservation

Reservation status

PENDING -> PICKED_UP -> RETURNED
PENDING -> CANCELLED

Decisions worth defending

- Branch owns its cars and indexes them by type, a search is a lookup inside one branch and not a scan of the fleet
- Car has no branchId, containment is the fact, holding both would let the two drift
- Double booking is stopped by Car.tryReserve claiming the range under the car's own lock, the branch index has nothing to do with it
- One way rentals would flip this, a car's branch would become mutable state and the index would have to be maintained on transfer

Swappable
- pricing (per day here, per hour or seasonal is the same interface)
- which car to hand out when several are free

Not modelled
- payments, insurance, damage reports, one way rentals between branches
