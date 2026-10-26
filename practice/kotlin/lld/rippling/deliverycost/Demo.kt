package lld.rippling.deliverycost

import java.math.BigDecimal

fun main() {
    val tracker = DeliveryCostTracker()
    tracker.addDriver("d1", BigDecimal("10.00"))
    tracker.addDriver("d2", BigDecimal("20.00"))

    println("-- record deliveries (seconds since start of day)")
    tracker.recordDelivery("d1", 0, 5400)
    tracker.recordDelivery("d1", 3600, 7200)
    tracker.recordDelivery("d2", 1800, 3600)
    println("  total=${tracker.getTotalCost()}")

    println("-- pay up to 1h30")
    println("  paid now=${tracker.payUpTo(5400)} unpaid=${tracker.getUnpaidCost()}")

    println("-- paying the same cutoff again is a no-op")
    println("  paid now=${tracker.payUpTo(5400)} unpaid=${tracker.getUnpaidCost()}")

    println("-- pay the rest")
    println("  paid now=${tracker.payUpTo(10_000)} unpaid=${tracker.getUnpaidCost()}")

    println("-- over 3 hours is rejected")
    runCatching { tracker.recordDelivery("d1", 0, 4 * 3600) }.onFailure { println("  ${it.message}") }
}
