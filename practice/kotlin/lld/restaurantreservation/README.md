Restaurant reservation

- Restaurant has tables of different sizes and fixed opening hours
- Customer reserves a table for a party size at a start time; every reservation lasts one slot
- A table can't hold two reservations with overlapping intervals
- Concurrent reservations for the same table must not double book
- Customer can see which tables are free for a party size and time
- Reservation goes CONFIRMED -> SEATED -> COMPLETED, or is CANCELLED; completing or cancelling frees the table
- Customer can list their reservations

Entities

Customer
Table
TimeInterval
Reservation
Restaurant

Swappable
- which table to pick when several fit the party (smallest fit here)
