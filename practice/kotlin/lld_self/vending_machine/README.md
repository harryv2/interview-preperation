Vending Machine

- User will enter coin
- Machine will show current amount entered by user
- User can select slot to dispense
- Machine will dispense product from slot and return change
- Machine refuses a sale it can not give change for, before any stock moves
- Cancel returns exactly the coins that were inserted
- Machine goes out of service when every slot is empty, restock brings it back

Entities

Money
Coin
Product
Purchase
VendingSlot
Inventory
CoinBank
Sale
VendingMachine

Vending machine state

IDLE
COIN_INSERTED
DISPENSING
OUT_OF_SERVICE

Swappable
- how change is made from the coins on hand (greedy here)

Notes
- Sale owns the in progress transaction, the coins inserted and the slot chosen
- CoinBank owns the float only, it never holds a customer's coins
- Coins are banked and change is taken in one settle step, so money in = price + change out
