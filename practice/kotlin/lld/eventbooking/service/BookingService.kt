package lld.eventbooking.service

import lld.eventbooking.entity.Clock
import lld.eventbooking.entity.Money
import lld.eventbooking.entity.Occurrence
import lld.eventbooking.entity.Order
import lld.eventbooking.entity.OrderLine
import lld.eventbooking.entity.OrderStatus
import lld.eventbooking.entity.SystemClock
import lld.eventbooking.entity.TicketTier
import lld.eventbooking.strategy.PaymentMethod
import lld.eventbooking.strategy.PaymentResult
import lld.eventbooking.strategy.PricingStrategy
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


enum class BookingStatus {
    CONFIRMED,
    PAYMENT_DECLINED,
    PAYMENT_UNAVAILABLE,
    HOLD_EXPIRED,
    NOT_PAYABLE
}


class BookingResult(
    val status: BookingStatus,
    val order: Order,
    val message: String = ""
) {
    override fun toString(): String {
        val suffix = if (message.isEmpty()) "" else " — $message"
        return "$status$suffix"
    }
}


class BookingService(
    occurrences: List<Occurrence>,
    private val pricing: PricingStrategy,
    private val clock: Clock = SystemClock(),
    private val holdWindow: Duration = Duration.ofMinutes(8)
) {

    private val lock = ReentrantLock()
    private val occurrencesById = occurrences.associateBy { it.id }
    private val orders = mutableMapOf<String, Order>()
    private val paidKeys = mutableMapOf<String, String>()

    fun occurrencesOf(eventId: String): List<Occurrence> {
        return occurrencesById.values.filter { it.event.id == eventId }.sortedBy { it.startsAt }
    }

    fun availability(occurrenceId: String): Map<String, Int> {
        return occurrence(occurrenceId).availability(clock.now())
    }

    fun tiers(occurrenceId: String): List<TicketTier> {
        return occurrence(occurrenceId).tiers()
    }

    fun startOrder(userId: String, occurrenceId: String, lines: List<OrderLine>): Order {
        lock.withLock {
            val occurrence = occurrence(occurrenceId)
            val now = clock.now()
            require(now < occurrence.startsAt) { "${occurrence.event.title} has already started" }

            val wanted = lines.sumOf { it.quantity }
            val alreadyHeld = ticketsOn(userId, occurrence.event.id, now)
            require(alreadyHeld + wanted <= occurrence.event.maxTicketsPerUser) {
                "Limit is ${occurrence.event.maxTicketsPerUser} per person for ${occurrence.event.title}, " +
                    "you already have $alreadyHeld"
            }

            // priced before the hold, otherwise under demand pricing the buyer pays for their own demand
            val total = totalFor(occurrence, lines, now)

            val orderId = "O-${UUID.randomUUID().toString().take(8)}"
            val until = now.plus(holdWindow)
            val tickets = occurrence.hold(lines, orderId, until, now)

            val order = Order(
                id = orderId,
                userId = userId,
                occurrenceId = occurrence.id,
                eventId = occurrence.event.id,
                tickets = tickets,
                total = total,
                heldUntil = until,
                createdAt = now
            )
            orders[order.id] = order
            return order
        }
    }

    fun pay(orderId: String, method: PaymentMethod, idempotencyKey: String): BookingResult {
        lock.withLock {
            // a retried request from a client that never saw the response must not buy the tickets twice
            paidKeys[idempotencyKey]?.let {
                return BookingResult(BookingStatus.CONFIRMED, order(it), "replay of an earlier payment")
            }

            val order = order(orderId)
            val now = clock.now()

            if (order.status != OrderStatus.HELD) {
                return BookingResult(BookingStatus.NOT_PAYABLE, order, "the order is already ${order.status}")
            }

            if (!order.isLive(now)) {
                expire(order)
                return BookingResult(BookingStatus.HOLD_EXPIRED, order, "the hold ran out and the tickets went back on sale")
            }

            // a declined leg deliberately leaves the hold alone, the buyer gets the rest of the window for another card
            return when (val result = method.pay(order.total, order.id)) {
                is PaymentResult.Success -> {
                    occurrence(order.occurrenceId).confirm(order.id)
                    order.markConfirmed(result.payment)
                    paidKeys[idempotencyKey] = order.id
                    BookingResult(BookingStatus.CONFIRMED, order)
                }
                is PaymentResult.Declined -> BookingResult(BookingStatus.PAYMENT_DECLINED, order, result.reason)
                is PaymentResult.Unavailable -> BookingResult(BookingStatus.PAYMENT_UNAVAILABLE, order, result.reason)
            }
        }
    }

    fun cancel(orderId: String): Order {
        lock.withLock {
            val order = order(orderId)
            order.markCancelled()
            occurrence(order.occurrenceId).release(order.id)
            return order
        }
    }

    // lazy expiry already keeps reads honest, this is what actually hands the inventory back
    fun sweepExpired(): Int {
        lock.withLock {
            val now = clock.now()
            val dead = orders.values.filter { it.status == OrderStatus.HELD && !it.isLive(now) }

            dead.forEach { expire(it) }
            occurrencesById.values.forEach { it.sweep(now) }
            return dead.size
        }
    }

    fun ordersOf(userId: String): List<Order> {
        lock.withLock {
            return orders.values.filter { it.userId == userId }.sortedBy { it.createdAt }
        }
    }

    private fun expire(order: Order) {
        order.markExpired()
        occurrence(order.occurrenceId).release(order.id)
    }

    private fun ticketsOn(userId: String, eventId: String, now: Instant): Int {
        return orders.values
            .filter { it.userId == userId && it.eventId == eventId && it.countsAgainstLimit(now) }
            .sumOf { it.ticketCount }
    }

    private fun totalFor(occurrence: Occurrence, lines: List<OrderLine>, now: Instant): Money {
        return lines.fold(Money.ZERO) { sum, line ->
            val tier = occurrence.tier(line.tierId)
            sum + pricing.priceFor(tier, line.quantity, occurrence.soldFraction(line.tierId, now))
        }
    }

    private fun occurrence(occurrenceId: String): Occurrence {
        return requireNotNull(occurrencesById[occurrenceId]) { "No occurrence $occurrenceId" }
    }

    private fun order(orderId: String): Order {
        return requireNotNull(orders[orderId]) { "No order $orderId" }
    }
}
