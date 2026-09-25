package lld.orderbook

import lld.orderbook.entity.Price
import lld.orderbook.entity.Side
import lld.orderbook.service.MatchingEngine

fun main() {

    val engine = MatchingEngine("BTC-INR")

    banner("build a book, nothing crosses yet")
    engine.limit("alice", Side.BUY, Price.of(100), 10)
    engine.limit("bob", Side.BUY, Price.of(99), 5)
    engine.limit("carol", Side.SELL, Price.of(102), 8)
    engine.limit("dave", Side.SELL, Price.of(103), 12)
    show(engine)

    banner("a limit buy that crosses, and gets the resting price")
    val (taker, fills) = engine.limit("eve", Side.BUY, Price.of(102, 50), 10)
    fills.forEach { println("  $it") }
    println("  $taker")
    println("  eve bid 102.50 but paid 102.00, the resting price")
    show(engine)

    banner("price time priority: same price, oldest fills first")
    val fifo = MatchingEngine("BTC-INR")
    val (early, _) = fifo.limit("frank", Side.SELL, Price.of(105), 5)
    val (late, _) = fifo.limit("grace", Side.SELL, Price.of(105), 5)
    println("  ${early.id} rested before ${late.id}, both 5 @ 105.00")
    fifo.limit("heidi", Side.BUY, Price.of(105), 5).second.forEach { println("  $it") }
    println("  $early")
    println("  $late   <- untouched, it queued behind")

    banner("a market sell walks the bid side")
    val (mkt, marketFills) = engine.market("ivan", Side.SELL, 12)
    marketFills.forEach { println("  $it") }
    println("  $mkt")
    show(engine)

    banner("a market order eats the book, the rest is cancelled not rested")
    val (huge, partial) = engine.market("judy", Side.BUY, 1000)
    println("  filled ${huge.filled} across ${partial.size} trades, then $huge")
    println("  asks left: ${engine.book().depth(Side.SELL)}")

    banner("a market order into an empty side just cancels")
    val bare = MatchingEngine("BTC-INR")
    val (empty, none) = bare.market("judy", Side.BUY, 10)
    println("  fills: ${none.size}, order is $empty")

    banner("cancel pulls an order out of the book")
    val (pulled, _) = engine.limit("ken", Side.BUY, Price.of(90), 7)
    println("  before cancel, bids: ${engine.book().depth(Side.BUY)}")
    println("  cancelled: ${engine.cancel(pulled.id)}, again: ${engine.cancel(pulled.id)}")
    println("  after cancel,  bids: ${engine.book().depth(Side.BUY)}")

    banner("rejected")
    attempt("zero quantity") {
        engine.limit("mallory", Side.BUY, Price.of(100), 0)
    }
    println("  total trades printed: ${engine.book().tradeHistory().size}")
}

private fun show(engine: MatchingEngine) {
    val book = engine.book()
    println("  bids ${book.depth(Side.BUY)}")
    println("  asks ${book.depth(Side.SELL)}")
    println("  spread: ${book.bestBid()} / ${book.bestAsk()}")
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
