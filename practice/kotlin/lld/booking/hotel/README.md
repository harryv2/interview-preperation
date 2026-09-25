Hotel Management System

1. Requirements

In scope
- Rooms of several types, each type with a nightly rate
- A guest books a room TYPE for a date range, the hotel assigns the actual room at check in
- Never sell more rooms of a type than exist on any night of a stay
- Check in, check out, cancel
- Quote a stay, where what a night costs is a policy the hotel can change
- Several properties behind one service: search a city, book at one of them, and reach a stay by its id

Out of scope
- payments, refunds, cancellation fees, deposits, taxes. See the note under Extensibility
- housekeeping and maintenance holds, upgrades, walk ins, guest room requests
- channel managers, overbooking, loyalty pricing, one guest's stays being checked against each other

2. Entities

DateRange      checkIn, checkOut, half open
RoomType       SINGLE, DOUBLE, SUITE, each with a base rate
Room           number, type, and the stays actually assigned to it
Guest          id, name
Reservation    hotel, guest, TYPE, stay, total, status, and a room once one is handed over
Hotel          one property: its rooms and its reservations
BookingService the platform above the properties, routing only
Offer          what one property answers a search with, a snapshot that holds nothing
PricingPolicy  what a stay costs

A reservation is against a type and carries no room until check in. A room only learns about a stay when it is
handed to someone at the desk.

3. Class design

    DateRange(checkIn, checkOut)
      nights: Int
      overlaps(other): Boolean
      covers(night): Boolean

    BookingService()
      register(hotel)
      hotelsIn(city): List<Hotel>
      search(city, type, stay): List<Offer>        <- fans out, cheapest first
      book(guest, hotelId, type, stay): Reservation
      checkIn / checkOut / cancel(reservationId)   <- routed to the property that sold it
      reservationsOf(guest): List<Reservation>

    Hotel(id, name, city, rooms, pricing, clock)
      freeRooms(type, stay): Int
      availability(stay): Map<RoomType, Int>
      quote(type, stay): Money
      book(guest, type, stay): Reservation
      checkIn(reservationId): Reservation      <- assigns the room
      checkOut / cancel(reservationId): Reservation

    Room(number, type)
      isFree(stay): Boolean
      hold(reservationId, stay)

    PricingPolicy
      quote(type, stay): Money      FlatRate, WeekendSurcharge

4. Key decisions

The date range is half open, and that is the first half of the problem. A stay occupies the NIGHTS from
checkIn up to checkOut - 1. The guest leaving on the 13th and the guest arriving on the 13th are not in
conflict, and a design that treats a stay as a set of days refuses that booking.

    a.checkIn < b.checkOut && b.checkIn < a.checkOut

Selling a type rather than a room is the second half, and it changes the availability question completely.
There is no room to ask "are you free", so the question becomes how many rooms of this type are committed on
each night of the range, and the answer is the PEAK across those nights, not a sum:

    peak = max over nights of (reservations of this type covering that night)
    free = rooms of that type - peak

Demo shows why: with two doubles booked for the 9th to the 13th and one for the 13th to the 16th, a request
for the 11th to the 14th is refused because nights 11 and 12 are full, even though night 13 has a room spare.

First fit at check in can never get stuck. Guests arrive in check in date order, and first fit by start time
colours an interval graph in exactly peak-overlap many rooms. The booking rule caps peak overlap at the number
of rooms of that type, so a room is always there when someone walks up to the desk. checkIn asserts this
rather than handling it, because if it ever fails the inventory count is wrong and that is a bug, not a case.

Only a cancellation frees inventory, and it frees it by existing rather than by releasing anything. A
CONFIRMED booking has no room to hand back, so cancel just flips the status and the count stops including it.

Booking is check then act, so Hotel holds one lock. freeRooms followed by book is exactly the race where two
guests both see the last double free.

The service routes and owns no inventory, which is the whole point of the split. It knows which properties
exist and which one a reservation id came from, and every real decision happens inside a Hotel under that
hotel's own lock. A busy property slows down its own answers and nobody else's, which a service level lock
would have thrown away. search is deliberately not atomic across properties: an Offer is a snapshot that holds
nothing, and book re checks under the seller's lock, so a stale offer turns into a rejection and never into an
oversell. A stay records its property on the Reservation, because that is part of what the booking IS and not
something the service happens to know. The service still keeps a reservation id to hotel map, which is an
index and not a second copy of the truth: a desk call arrives as a bare id with no reservation in hand, so
something has to turn that id into a property before anyone can read the field on it. Ids are prefixed with
the property so six characters stay unique once more than one hotel is issuing them, but nothing routes by
parsing them.

A key is only handed over between the arrival date and the last night, so a guest cannot check in early and
hold a physical room for a stay that starts in three weeks. That rule is also what makes the first fit
argument above true, since it is what forces guests to arrive in check in date order. The clock is injected
rather than read from the system, so the rule is testable.

Pricing is the one interface, and it prices a TYPE, because at booking time no room has been picked. It is the
thing a hotel actually changes, weekends and seasons and length of stay, and it is why DateRange hands out its
dates rather than just a night count. The quote is frozen on the reservation, so a later rate change does not
move a booked stay.

5. Extensibility

- payment: the money hooks are already where they need to be. The total is frozen on the Reservation at
  booking, cancel() is where a fee policy would read the stay's start date, and checkOut() is where the charge
  lands. A card guarantee at booking is a PaymentMethod call in book(). It is deliberately not built, because
  done properly it needs a gateway interface, declined and unavailable as different outcomes, an idempotency
  key so a retried request does not charge twice, and refunds. That is a second system and it is not what this
  problem is testing
- seasonal or length of stay pricing: another PricingPolicy, nothing else moves
- overbooking: freeRooms returns rooms * (1 + factor) - peak, and check in gains a walk case
- a guest requesting a specific room: a preference on the Reservation that check in tries to honour, which
  first fit can ignore without breaking
- maintenance holds: a fake reservation against a room, or a blocked range on Room that isFree also checks
- a guest double booking themselves: the service sees every property, so it is the only layer that can catch
  two overlapping stays for one guest. It is deliberately not built, because a platform that books for
  families and colleagues has no business refusing it without a rule about who the stay is for
- search at real scale: fanning out to every property in a city is fine for a demo and wrong for a platform.
  That becomes an availability index the service reads, with the hotel still the authority at book time
- long ranges and real volume: freeRooms is linear in reservations of that type times nights. A property with
  years of history wants the commitments in a per night counter table instead
- persistence and more than one server: the in process lock stops being enough and the cap has to move into
  the database as a transaction or a constraint
