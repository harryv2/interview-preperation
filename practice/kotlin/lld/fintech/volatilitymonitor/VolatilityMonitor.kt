package lld.fintech.volatilitymonitor

import lld.fintech.volatilitymonitor.entity.Price
import lld.fintech.volatilitymonitor.entity.Snapshot
import lld.fintech.volatilitymonitor.structure.MonotonicWindow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// One symbol's stream. Every question it answers is a sliding window extreme, so each one is a monotonic
// window fed on the way past and read off the front.
class VolatilityMonitor(
    val symbol: String,
    private val windowSize: Int
) {

    init {
        require(windowSize >= 2) { "A window of $windowSize trades has no price change in it" }
    }

    private val lock = ReentrantLock()

    private val highs = MonotonicWindow.forMax(windowSize)
    private val lows = MonotonicWindow.forMin(windowSize)

    // N trades produce N - 1 consecutive moves, so the move window is one shorter than the price window.
    // Sizing both the same is the off by one that silently reports a move from a trade that has left.
    private val moves = MonotonicWindow.forMax(windowSize - 1)

    private var last: Price? = null
    private var seen = 0L

    fun record(price: Price) {
        lock.withLock {
            last?.let { moves.add((price - it).abs().paise) }

            highs.add(price.paise)
            lows.add(price.paise)
            last = price
            seen += 1
        }
    }

    // the biggest single trade to trade jump still inside the window, which is the volatility the question
    // asks for. Null until two trades have landed, because one trade is not a change.
    fun maxTickMove(): Price? {
        lock.withLock {
            return moves.extreme()?.let { Price(it) }
        }
    }

    // the other reading of volatility, high minus low across the window. Free once the extremes are already
    // being tracked, and the two disagree in the case worth seeing: a slow grind is a wide range with tiny
    // ticks, a spike and a recovery is a narrow range with one huge tick
    fun range(): Price? {
        lock.withLock {
            val high = highs.extreme() ?: return null
            val low = lows.extreme() ?: return null
            return Price(high - low)
        }
    }

    fun snapshot(): Snapshot? {
        lock.withLock {
            val current = last ?: return null
            return Snapshot(
                symbol = symbol,
                trades = minOf(seen, windowSize.toLong()).toInt(),
                last = current,
                high = Price(highs.extreme()!!),
                low = Price(lows.extreme()!!),
                maxTickMove = moves.extreme()?.let { Price(it) } ?: Price(0)
            )
        }
    }
}
