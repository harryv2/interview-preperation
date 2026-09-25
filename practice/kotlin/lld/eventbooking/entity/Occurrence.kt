package lld.eventbooking.entity

import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// one date of an event at one venue, it owns its inventory and the lock over it
class Occurrence(
    val id: String,
    val event: Event,
    val venue: Venue,
    val startsAt: Instant,
    inventories: List<Inventory>
) {

    private val lock = ReentrantLock()
    private val byTier = inventories.associateBy { it.tier.id }

    fun tiers(): List<TicketTier> {
        return byTier.values.map { it.tier }
    }

    fun tier(tierId: String): TicketTier {
        return inventory(tierId).tier
    }

    fun availability(now: Instant): Map<String, Int> {
        lock.withLock {
            return byTier.values.associate { it.tier.name to it.available(now) }
        }
    }

    fun availableIn(tierId: String, now: Instant): Int {
        lock.withLock {
            return inventory(tierId).available(now)
        }
    }

    // all or nothing across tiers, anything already taken is handed back before the failure leaves this method
    fun hold(lines: List<OrderLine>, holdId: String, until: Instant, now: Instant): List<TicketRef> {
        require(lines.isNotEmpty()) { "Nothing selected" }
        require(lines.distinctBy { it.tierId }.size == lines.size) { "Put one line per tier" }

        lock.withLock {
            val taken = mutableListOf<TicketRef>()
            val touched = mutableListOf<Inventory>()

            try {
                lines.forEach {
                    val inventory = inventory(it.tierId)
                    touched.add(inventory)
                    taken.addAll(inventory.hold(it, holdId, until, now))
                }
            } catch (e: RuntimeException) {
                touched.forEach { it.release(holdId) }
                throw e
            }

            return taken
        }
    }

    fun confirm(holdId: String) {
        lock.withLock {
            byTier.values.forEach { it.confirm(holdId) }
        }
    }

    fun release(holdId: String) {
        lock.withLock {
            byTier.values.forEach { it.release(holdId) }
        }
    }

    // what the tier has already given up, the demand price is a function of this
    fun soldFraction(tierId: String, now: Instant): Double {
        lock.withLock {
            val inventory = inventory(tierId)
            if (inventory.capacity == 0) {
                return 0.0
            }
            return (inventory.capacity - inventory.available(now)).toDouble() / inventory.capacity
        }
    }

    fun sweep(now: Instant): Int {
        lock.withLock {
            return byTier.values.sumOf { it.sweep(now) }
        }
    }

    private fun inventory(tierId: String): Inventory {
        return requireNotNull(byTier[tierId]) { "No tier $tierId on ${event.title}" }
    }

    override fun toString(): String {
        return "${event.title} at ${venue.name} on $startsAt"
    }
}
