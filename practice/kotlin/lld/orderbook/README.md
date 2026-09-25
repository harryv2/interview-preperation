Order Book Matching Engine

- Traders submit LIMIT or MARKET orders on one symbol
- Resting orders sit in the book, bids high to low, asks low to high
- Price time priority: best price first, and within a price the oldest order first
- An incoming order matches against the opposite side while the prices cross
- A trade prints at the resting order's price, so the aggressor gets any price improvement
- A LIMIT order's unfilled remainder rests in the book, a MARKET order's is cancelled
- The book is never crossed after a submit: best bid is always below best ask

Entities

Price
Side
OrderType
Order
Trade
OrderBook

Invariants worth stating in an interview
- quantity is conserved: for every trade, taker filled == maker filled
- an order's filled + remaining always equals its original quantity
- the book is never crossed once submit returns

Structure
- two TreeMaps price -> FIFO queue. TreeMap gives the best price in O(log n), the deque gives
  time priority in O(1). A plain sorted list of orders would make cancel and best price O(n).

Not modelled
- multiple symbols, stop and iceberg orders, self trade prevention, fees, market data feed
