# Delivery Driver Cost

Very common Rippling screen (2024 to 2026 reports).

1. `addDriver(driverId, hourlyRate)`
2. `recordDelivery(driverId, startTime, endTime)` — second precision, max 3 hours per delivery
3. `getTotalCost()` — sum over all deliveries of duration × rate
4. follow-up: `payUpTo(time)` pays every delivery that ended at or before `time`, `getUnpaidCost()` returns the rest

Design:
- `Driver`, `Delivery` — plain data. Cost is computed once when the delivery is recorded, so a later rate change does not touch old deliveries
- `DeliveryCostTracker` — running `totalCost` and `paidCost`, so both getters are O(1). Unpaid deliveries sit in a min heap by end time; `payUpTo` pops while `end <= time`. Calling it twice with the same time is a no-op
- Money is `BigDecimal`, never `Double`. Say why: 0.1 + 0.2

Overlapping deliveries for one driver are allowed and each is paid on its own.

Follow-ups: cost per driver (map driverId -> total), rate changes with an effective date (list of (from, rate) per driver, split a delivery across the boundary), thread safety (synchronize the three mutating calls).
