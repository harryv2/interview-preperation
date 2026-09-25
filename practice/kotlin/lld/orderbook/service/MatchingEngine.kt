package lld.orderbook.service

import lld.orderbook.entity.Order
import lld.orderbook.entity.OrderType
import lld.orderbook.entity.Price
import lld.orderbook.entity.Side
import lld.orderbook.entity.Trade
import java.util.concurrent.atomic.AtomicLong


class MatchingEngine(
    symbol: String
) {

    private val book = OrderBook(symbol)
    private val sequence = AtomicLong()

    fun limit(trader: String, side: Side, price: Price, quantity: Long): Pair<Order, List<Trade>> {
        val order = newOrder(trader, side, OrderType.LIMIT, price, quantity)
        return order to book.submit(order)
    }

    fun market(trader: String, side: Side, quantity: Long): Pair<Order, List<Trade>> {
        val order = newOrder(trader, side, OrderType.MARKET, null, quantity)
        return order to book.submit(order)
    }

    fun cancel(orderId: String): Boolean {
        return book.cancel(orderId)
    }

    fun book(): OrderBook {
        return book
    }

    private fun newOrder(trader: String, side: Side, type: OrderType, price: Price?, quantity: Long): Order {
        val id = "O${sequence.incrementAndGet()}"
        return Order(id, trader, side, type, price, quantity, sequence.get())
    }
}
