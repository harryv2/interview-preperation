Parking slot booking

- Multiple companies book slots in one parking lot for a time interval
- A slot can't have two bookings with overlapping intervals
- Concurrent bookings for the same slot must not double book
- Company can cancel a booking, slot becomes free for that interval
- Company can see which slots are free for an interval

Entities

Company
Slot
Booking
TimeInterval
ParkingLot

Swappable
- which slot to pick when multiple are free
