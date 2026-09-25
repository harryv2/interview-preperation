package lld.fintech.volatilitymonitor.service

import lld.fintech.volatilitymonitor.VolatilityMonitor
import lld.fintech.volatilitymonitor.entity.Price
import lld.fintech.volatilitymonitor.entity.Snapshot
import java.util.concurrent.ConcurrentHashMap


// Many symbols, one per monitor. Sharding by symbol is the only concurrency design a tape needs: two symbols
// share no state, so two threads on different symbols never meet, and one symbol is a single ordered stream
// by definition.
class MarketMonitor(private val windowSize: Int) {

    private val monitors = ConcurrentHashMap<String, VolatilityMonitor>()

    fun record(symbol: String, price: Price) {
        monitors.computeIfAbsent(symbol) { VolatilityMonitor(it, windowSize) }.record(price)
    }

    fun snapshot(symbol: String): Snapshot? {
        return monitors[symbol]?.snapshot()
    }

    // the noisiest names right now, which is the question a trading desk actually asks of this thing
    fun mostVolatile(limit: Int): List<Snapshot> {
        return monitors.values
            .mapNotNull { it.snapshot() }
            .sortedByDescending { it.maxTickMove.paise }
            .take(limit)
    }

    fun symbols(): List<String> = monitors.keys.sorted()
}
