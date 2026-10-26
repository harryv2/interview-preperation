package lld.rippling.deliverycost

import lld.rippling.deliverycost.entity.Delivery
import lld.rippling.deliverycost.entity.Driver
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.PriorityQueue

class DeliveryCostTracker(private val maxDurationSec: Long = 3 * 60 * 60) {
    private val drivers = HashMap<String, Driver>()
    private val unpaid = PriorityQueue<Delivery>(compareBy { it.endSec })
    private var totalCost = BigDecimal.ZERO
    private var paidCost = BigDecimal.ZERO

    fun addDriver(driverId: String, hourlyRate: BigDecimal) {
        require(!drivers.containsKey(driverId)) { "Driver $driverId already exists" }
        require(hourlyRate >= BigDecimal.ZERO) { "Rate cannot be negative" }
        drivers[driverId] = Driver(driverId, hourlyRate)
    }

    fun recordDelivery(driverId: String, startSec: Long, endSec: Long): Delivery {
        val driver = requireNotNull(drivers[driverId]) { "No driver $driverId" }
        require(endSec > startSec) { "End must be after start" }
        require(endSec - startSec <= maxDurationSec) { "Delivery longer than $maxDurationSec seconds" }

        val cost = driver.hourlyRate
            .multiply(BigDecimal.valueOf(endSec - startSec))
            .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP)

        val delivery = Delivery(driverId, startSec, endSec, cost)
        unpaid += delivery
        totalCost += cost
        return delivery
    }

    fun getTotalCost(): BigDecimal {
        return totalCost
    }

    fun payUpTo(timeSec: Long): BigDecimal {
        var paidNow = BigDecimal.ZERO
        while (unpaid.isNotEmpty() && unpaid.peek().endSec <= timeSec) {
            paidNow += unpaid.poll().cost
        }
        paidCost += paidNow
        return paidNow
    }

    fun getUnpaidCost(): BigDecimal {
        return totalCost - paidCost
    }
}
