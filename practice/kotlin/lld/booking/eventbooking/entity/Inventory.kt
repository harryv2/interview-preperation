package lld.booking.eventbooking.entity

import java.time.Instant


class SoldOutException(tierName: String, wanted: Int, left: Int) :
    RuntimeException("$tierName has $left left, you asked for $wanted")

class SeatTakenException(label: String) :
    RuntimeException("Seat $label is already taken")


// one inventory per tier on one occurrence, every method runs under the occurrence lock
interface Inventory {

    val tier: TicketTier

    val capacity: Int

    fun available(now: Instant): Int

    fun hold(line: OrderLine, holdId: String, until: Instant, now: Instant): List<TicketRef>

    fun confirm(holdId: String)

    fun release(holdId: String)

    fun sweep(now: Instant): Int
}


// reserved seating, the buyer picks the seat and the seat is the unit of inventory
class SeatedInventory(
    override val tier: TicketTier,
    seats: List<Seat>
) : Inventory {

    private val states = seats.associate { it.id to SeatState(it) }
    private val heldSeats = mutableMapOf<String, List<SeatState>>()

    override val capacity = states.size

    override fun available(now: Instant): Int {
        return states.values.count { it.isFree(now) }
    }

    override fun hold(line: OrderLine, holdId: String, until: Instant, now: Instant): List<TicketRef> {
        require(line.seatIds.isNotEmpty()) { "${tier.name} is reserved seating, pick your seats" }
        require(line.seatIds.distinct().size == line.seatIds.size) { "The same seat twice in one order" }

        // every seat is checked before any seat is taken, a partial grab is worse than a clean refusal
        val picked = line.seatIds.map {
            val state = states[it]
            requireNotNull(state) { "Seat $it is not in ${tier.name}" }
            if (!state.isFree(now)) {
                throw SeatTakenException(state.seat.label)
            }
            state
        }

        picked.forEach { it.hold(holdId, until) }
        heldSeats[holdId] = picked
        return picked.map { TicketRef(tier.id, tier.name, it.seat.label) }
    }

    override fun confirm(holdId: String) {
        heldSeats.remove(holdId)?.forEach { it.confirm(holdId) }
    }

    override fun release(holdId: String) {
        heldSeats.remove(holdId)?.forEach { it.release(holdId) }
    }

    override fun sweep(now: Instant): Int {
        val dead = heldSeats.filterValues { seats -> seats.any { it.hasExpired(now) } }
        dead.keys.forEach { release(it) }
        return dead.size
    }
}


// general admission, nobody has a seat so the unit of inventory is a count against a capacity
class StandingInventory(
    override val tier: TicketTier,
    override val capacity: Int
) : Inventory {

    private val holds = mutableMapOf<String, StandingHold>()
    private var sold = 0

    override fun available(now: Instant): Int {
        val outstanding = holds.values.filter { !it.hasExpired(now) }.sumOf { it.quantity }
        return capacity - sold - outstanding
    }

    override fun hold(line: OrderLine, holdId: String, until: Instant, now: Instant): List<TicketRef> {
        require(line.seatIds.isEmpty()) { "${tier.name} is standing, there are no seats to pick" }
        require(line.quantity > 0) { "Quantity must be positive" }

        val left = available(now)
        if (line.quantity > left) {
            throw SoldOutException(tier.name, line.quantity, left)
        }

        holds[holdId] = StandingHold(line.quantity, until)
        return List(line.quantity) { TicketRef(tier.id, tier.name, null) }
    }

    override fun confirm(holdId: String) {
        val hold = holds.remove(holdId) ?: return
        sold += hold.quantity
    }

    override fun release(holdId: String) {
        holds.remove(holdId)
    }

    override fun sweep(now: Instant): Int {
        val dead = holds.filterValues { it.hasExpired(now) }
        dead.keys.forEach { holds.remove(it) }
        return dead.size
    }
}


private class StandingHold(
    val quantity: Int,
    val until: Instant
) {
    fun hasExpired(now: Instant): Boolean {
        return until < now
    }
}


private class SeatState(val seat: Seat) {

    private var holdId: String? = null
    private var until: Instant? = null
    private var sold: Boolean = false

    fun isFree(now: Instant): Boolean {
        if (sold) {
            return false
        }
        if (holdId == null) {
            return true
        }
        return hasExpired(now)
    }

    fun hasExpired(now: Instant): Boolean {
        val deadline = until ?: return false
        return deadline < now
    }

    fun hold(holdId: String, until: Instant) {
        this.holdId = holdId
        this.until = until
    }

    // an expired hold can already have been overwritten by the next buyer, so both of these check whose hold it is
    fun confirm(byHoldId: String) {
        if (holdId != byHoldId) {
            return
        }
        sold = true
        until = null
    }

    fun release(byHoldId: String) {
        if (sold || holdId != byHoldId) {
            return
        }
        holdId = null
        until = null
    }
}
