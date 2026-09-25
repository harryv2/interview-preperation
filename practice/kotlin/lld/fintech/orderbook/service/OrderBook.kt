package lld.fintech.orderbook.service

import lld.fintech.orderbook.entity.BookLevel
import lld.fintech.orderbook.entity.Order
import lld.fintech.orderbook.entity.OrderStatus
import lld.fintech.orderbook.entity.OrderType
import lld.fintech.orderbook.entity.Price
import lld.fintech.orderbook.entity.Side
import lld.fintech.orderbook.entity.Trade
import java.util.TreeMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class OrderBook(
    val symbol: String
) {

    private val lock = ReentrantLock()
    private val bids = TreeMap<Price, ArrayDeque<Order>>(reverseOrder())
    private val asks = TreeMap<Price, ArrayDeque<Order>>()
    private val resting = HashMap<String, Order>()
    private val trades = mutableListOf<Trade>()
    private var sequence = 0L

    fun submit(order: Order): List<Trade> {
        lock.withLock {
            val fills = match(order)

            if (order.remaining > 0) {
                if (order.type == OrderType.LIMIT) {
                    rest(order)
                } else {
                    order.cancel()
                }
            }

            return fills
        }
    }

    fun cancel(orderId: String): Boolean {
        lock.withLock {
            val order = resting[orderId] ?: return false
            val book = bookFor(order.side)
            val price = order.limitPrice!!

            val queue = book[price] ?: return false
            queue.remove(order)
            if (queue.isEmpty()) {
                book.remove(price)
            }
            resting.remove(orderId)
            order.cancel()
            return true
        }
    }

    fun bestBid(): Price? {
        lock.withLock {
            return bids.firstEntry()?.key
        }
    }

    fun bestAsk(): Price? {
        lock.withLock {
            return asks.firstEntry()?.key
        }
    }

    fun depth(side: Side): List<BookLevel> {
        lock.withLock {
            return bookFor(side).map { (price, queue) ->
                BookLevel(price, queue.sumOf { it.remaining }, queue.size)
            }
        }
    }

    fun tradeHistory(): List<Trade> {
        lock.withLock {
            return trades.toList()
        }
    }

    private fun match(taker: Order): List<Trade> {

        val opposite = bookFor(taker.side.opposite())
        val fills = mutableListOf<Trade>()

        while (taker.remaining > 0 && opposite.isNotEmpty()) {
            val level = opposite.firstEntry()
            if (!taker.crossesWith(level.key)) {
                break
            }

            val queue = level.value
            while (taker.remaining > 0 && queue.isNotEmpty()) {
                val maker = queue.first()
                val quantity = minOf(taker.remaining, maker.remaining)

                taker.fill(quantity)
                maker.fill(quantity)

                val trade = Trade(
                    buyOrderId = if (taker.side == Side.BUY) taker.id else maker.id,
                    sellOrderId = if (taker.side == Side.SELL) taker.id else maker.id,
                    price = level.key,
                    quantity = quantity,
                    sequence = sequence++
                )
                fills.add(trade)
                trades.add(trade)

                if (maker.status == OrderStatus.FILLED) {
                    queue.removeFirst()
                    resting.remove(maker.id)
                }
            }

            if (queue.isEmpty()) {
                opposite.remove(level.key)
            }
        }

        return fills
    }

    private fun rest(order: Order) {
        val book = bookFor(order.side)
        book.getOrPut(order.limitPrice!!) { ArrayDeque() }.addLast(order)
        resting[order.id] = order
    }

    private fun bookFor(side: Side): TreeMap<Price, ArrayDeque<Order>> {
        return if (side == Side.BUY) bids else asks
    }
}
