Restaurant Order Management (single restaurant, dine in)

- Waiter opens a table on a handheld and an order is created against it
- Items are punched in rounds, every round becomes a KOT per station
- Kitchen and bar pull their own queue, accept a KOT and mark it ready
- Waiter serves each line, a line can be cancelled only before the station starts it
- Bill is printed once every line is served or cancelled, table is freed only after settlement

Entities

Money
MenuItem, Menu
DiningTable
OrderLine
Kot
Order
Bill

Line status

PLACED -> PREPARING -> READY -> SERVED
PLACED -> CANCELLED

Order status

OPEN -> BILLED -> CLOSED
BILLED -> OPEN (guest orders one more round after asking for the bill)

Decisions worth defending

- OrderLine carries unitPrice copied from the menu at punch time, a reprice mid meal never moves the bill
- Status lives on the line, not the order, starters are served while mains are still cooking
- One punch fans out into one KOT per station, order and ticket are different things
- Cancel window closes when the station starts, after that the food exists and is charged
- The punch carries a device minted id, a resend over flaky wifi does not cook the food twice
- Table is released on settle, not on bill

Swappable
- bill policy (dine in with service charge, takeaway without)
- discount policy

Not modelled
- rounding rules on tax and service charge, reservations, split bills, table merge and move, inventory depletion, shift and tip reports, kitchen load balancing
