Delivery slot booking (capacity counter)

Same problem as deliveryslot, modelled the way it fits a 45 minute round: a slot carries a capacity
counter derived from the van pool instead of modelling vans. Every state change is a conditional
update in the store, which is what a DB would do (UPDATE ... WHERE booked < capacity).

- Customer sees available slots for a zone and date range at checkout
- Slot has a fixed capacity; booking takes one unit, cancel gives it back
- Slots starting within the cutoff window are not shown or bookable
- An order holds at most one confirmed booking; booking the same slot again returns it, a
  different slot is rejected
- Two concurrent bookings on the last unit: one wins, the other gets SlotFull
- Two concurrent bookings for the same order: one wins, the loser releases its unit
- Cancel is idempotent and releases capacity exactly once

Entities

Slot
Booking

Swappable
- repositories (in-memory here); tryReserve / tryInsert / tryCancel are the atomic points
