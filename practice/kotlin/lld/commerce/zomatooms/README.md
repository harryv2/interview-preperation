Zomato Order Management System

Customer orders food from a restaurant. Restaurant accepts and cooks it, a delivery partner
picks it up and delivers it. This models that order lifecycle.

- Customer places an order of menu items from one open restaurant
- Bill is itemTotal + packaging + delivery + tax - discount
- Restaurant accepts or rejects a placed order, then marks it ready
- A free delivery partner is assigned when the order is ready
- Partner picks up and delivers; the partner is freed on delivery
- Customer can cancel until the order is out for delivery
- Only legal status transitions are allowed, terminal states never move
- Two orders must never be assigned the same partner

Entities

Money
MenuItem
Restaurant
OrderItem
Bill
Order
DeliveryPartner
Customer

Order status

PLACED -> ACCEPTED -> READY -> OUT_FOR_DELIVERY -> DELIVERED
PLACED -> REJECTED
PLACED / ACCEPTED / READY -> CANCELLED

Swappable
- discount applied to an order
- which free partner gets the order

Not modelled
- payments, refunds, ratings, restaurant search, live location tracking, scheduled orders

Note
- A transition map on the enum is used instead of one class per status: there are many
  statuses and almost no behaviour per status, so the state pattern would be 7 near empty
  classes. The vending machine goes the other way for the opposite reason.
