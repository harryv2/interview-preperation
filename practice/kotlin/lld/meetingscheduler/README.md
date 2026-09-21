# Meeting Scheduler

- `entity/` — `User`, `Room`, `TimeSlot` (`overlaps`), `Meeting`, `Calendar` (meetings for one user or room, `isFree(slot)`), `Scheduler`
- `strategies/` — `RoomSelectionStrategy`: `SmallestFitRoomStrategy`

## `schedule(title, organizer, attendees, slot)`

1. every attendee's calendar must be free for the slot
2. pick a free room with enough capacity (strategy)
3. create the meeting, add it to the room's and each attendee's calendar

All under one lock so two organizers can't double-book a room or a person.

## `freeSlots(people, duration, within, step)`

Walk the window in `step` increments and keep the candidates where everyone is free. O(slots × people × meetings); fine for a day. For scale, merge each person's busy intervals first and walk the gaps.

Follow-ups: recurring meetings (expand into instances), notifications on invite / cancel, time zones (store UTC, convert at the edge).
