package lld.fintech.volatilitymonitor

import lld.fintech.volatilitymonitor.entity.Price
import lld.fintech.volatilitymonitor.service.MarketMonitor

fun main() {

    println("== a window of 5 trades, so 4 consecutive moves ==")
    val infy = VolatilityMonitor("INFY", windowSize = 5)

    val tape = listOf(1500.00, 1502.50, 1501.00, 1530.00, 1528.00, 1529.00, 1531.50, 1532.00)
    tape.forEach { price ->
        infy.record(Price.rupees(price))
        println("  ${Price.rupees(price)} -> ${infy.snapshot()}")
    }
    println("  the ₹29 jump drops out once it is more than 4 moves old, and the answer falls to the next best")

    println("\n== one trade is not a change ==")
    val single = VolatilityMonitor("TCS", windowSize = 3)
    println("  before any trade: maxTickMove=${single.maxTickMove()} range=${single.range()}")
    single.record(Price.rupees(3200.00))
    println("  after one trade:  maxTickMove=${single.maxTickMove()} range=${single.range()}")
    single.record(Price.rupees(3205.00))
    println("  after two:        maxTickMove=${single.maxTickMove()} range=${single.range()}")

    println("\n== the two readings of volatility disagree, which is the point of keeping both ==")
    val grind = VolatilityMonitor("GRIND", windowSize = 5)
    listOf(100.00, 102.00, 104.00, 106.00, 108.00).forEach { grind.record(Price.rupees(it)) }
    println("  steady climb:     range=${grind.range()} maxTick=${grind.maxTickMove()}")

    val spike = VolatilityMonitor("SPIKE", windowSize = 5)
    listOf(100.00, 100.50, 108.00, 100.50, 100.00).forEach { spike.record(Price.rupees(it)) }
    println("  spike and back:   range=${spike.range()} maxTick=${spike.maxTickMove()}")
    println("  same range, very different tapes, and only maxTick tells them apart")

    println("\n== repeated prices leave the window correctly ==")
    val flat = VolatilityMonitor("FLAT", windowSize = 3)
    listOf(50.00, 50.00, 50.00, 47.00).forEach { flat.record(Price.rupees(it)) }
    println("  ${flat.snapshot()}, high is 50 because two of them are still in the window")
    flat.record(Price.rupees(47.00))
    flat.record(Price.rupees(47.00))
    println("  ${flat.snapshot()}, every 50 has now aged out")

    println("\n== many symbols, one monitor each ==")
    val market = MarketMonitor(windowSize = 4)
    market.record("INFY", Price.rupees(1500.00))
    market.record("INFY", Price.rupees(1502.00))
    market.record("TCS", Price.rupees(3200.00))
    market.record("TCS", Price.rupees(3245.00))
    market.record("WIPRO", Price.rupees(410.00))
    market.record("WIPRO", Price.rupees(410.75))

    println("  symbols: ${market.symbols()}")
    println("  noisiest right now:")
    market.mostVolatile(3).forEach { println("    $it") }

    println("\n== rejected up front ==")
    attempt("a window of 1 trade") { VolatilityMonitor("X", windowSize = 1) }
    attempt("a window of 0") { VolatilityMonitor("X", windowSize = 0) }
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
