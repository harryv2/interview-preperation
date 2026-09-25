package lld.eventbooking.entity

import java.time.Instant


enum class OrderStatus {
    HELD,
    CONFIRMED,
    EXPIRED,
    CANCELLED
}


// one line per tier, seated tiers carry the chosen seat ids and standing tiers carry only a count
class OrderLine(
    val tierId: String,
    val quantity: Int,
    val seatIds: List<String> = emptyList()
) {

    companion object {

        fun seats(tierId: String, seatIds: List<String>): OrderLine {
            return OrderLine(tierId, seatIds.size, seatIds)
        }

        fun quantity(tierId: String, count: Int): OrderLine {
            return OrderLine(tierId, count)
        }
    }
}


// a ticket is a tier plus, for reserved seating only, the seat it entitles you to
class TicketRef(
    val tierId: String,
    val tierName: String,
    val seatLabel: String?
) {
    override fun toString(): String {
        return if (seatLabel == null) tierName else "$tierName $seatLabel"
    }
}


class Order(
    val id: String,
    val userId: String,
    val occurrenceId: String,
    val eventId: String,
    val tickets: List<TicketRef>,
    val total: Money,
    val heldUntil: Instant,
    val createdAt: Instant
) {

    var status: OrderStatus = OrderStatus.HELD
        private set

    var payment: Payment? = null
        private set

    val ticketCount: Int
        get() = tickets.size

    // lazy expiry: a hold whose window has run out is already gone, whether or not a sweeper has noticed
    fun isLive(now: Instant): Boolean {
        return status == OrderStatus.HELD && now <= heldUntil
    }

    fun countsAgainstLimit(now: Instant): Boolean {
        return status == OrderStatus.CONFIRMED || isLive(now)
    }

    fun markConfirmed(payment: Payment) {
        check(status == OrderStatus.HELD) { "Order $id is $status" }
        this.payment = payment
        status = OrderStatus.CONFIRMED
    }

    fun markExpired() {
        check(status == OrderStatus.HELD) { "Order $id is $status" }
        status = OrderStatus.EXPIRED
    }

    fun markCancelled() {
        check(status == OrderStatus.HELD) { "Order $id is $status" }
        status = OrderStatus.CANCELLED
    }

    override fun toString(): String {
        return "Order ${id.take(6)} [$status] $total ${tickets.joinToString()}"
    }
}
