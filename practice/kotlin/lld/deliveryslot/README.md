Delivery slot booking

An e-commerce company delivers orders to customers' homes. At checkout, a customer picks a delivery slot,
for example "Tue 14:00-16:00". Each slot has limited capacity because the number of delivery vans is finite.

- Customer sees the list of available slots for an order at checkout
- Slots have predefined time ranges, generated for a rolling horizon of days
- Any delivery van can deliver any order
- A van is assigned to a slot for an order; a van carries at most `capacity` orders per slot
- An order can hold one confirmed booking at a time
- Booking can be cancelled, which frees the van capacity for that slot
- Booking can be rescheduled to another slot
- Slots starting within the cutoff window are not bookable
- Concurrent bookings on the same slot must not exceed van capacity

Entities

Customer
Order
Warehouse
DeliveryVan
TimeSlot
Booking

Swappable
- which van to assign when several have room (first fit, least loaded)
- repositories (in-memory here)
