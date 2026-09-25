Inventory management

- Products are identified by SKU and have a reorder level
- Stock for a product is held per warehouse
- Stock comes in (receive), goes out (ship), and moves between warehouses (transfer)
- An order reserves stock first; the reservation is later confirmed (stock leaves) or released (stock returns)
- Reserved stock is not available to other orders
- Stock never goes negative; concurrent updates on the same warehouse/product are safe
- Every stock change is recorded in a ledger
- When available stock drops to the reorder level, listeners are notified

Entities

Product
Warehouse
StockItem
Reservation
StockMovement
InventoryService

Swappable
- which warehouse to reserve from when several have enough stock (most stock first here)
- low stock listeners
