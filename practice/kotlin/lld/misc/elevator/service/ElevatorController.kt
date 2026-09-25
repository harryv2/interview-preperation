package lld.misc.elevator.service

import lld.misc.elevator.entity.Direction
import lld.misc.elevator.entity.Elevator
import lld.misc.elevator.entity.Request
import lld.misc.elevator.scheduler.NearestCarStrategy
import lld.misc.elevator.scheduler.SchedulingStrategy

// =====================================================================
//  CONTROLLER
//
//  THE central decision of this design: the controller HOLDS hall
//  requests until they are served, and re-runs assignment every tick.
//
//  Fire-and-forget assignment broke three ways -- all cars busy meant
//  the request vanished, a car going to maintenance stranded it, and a
//  closer car freeing up came too late. Keeping the request fixes all
//  three, and it needed no change inside Elevator.
// =====================================================================

class ElevatorController(
    private val elevators: List<Elevator>,
    private var strategy: SchedulingStrategy = NearestCarStrategy()
) {
    /** request -> the car currently assigned to it, or null if unassigned. */
    private val pending = LinkedHashMap<Request, Elevator?>()

    fun requestElevator(floor: Int, direction: Direction) {
        // Duplicate presses collapse, because Request is a value.
        pending.putIfAbsent(Request(floor, direction), null)
    }

    /** Drives the hall button light. */
    fun isRequestPending(floor: Int, direction: Direction): Boolean =
        pending.containsKey(Request(floor, direction))

    fun pendingRequests(): List<Request> = pending.keys.toList()

    fun setStrategy(newStrategy: SchedulingStrategy) {
        strategy = newStrategy
    }

    fun elevators(): List<Elevator> = elevators

    /**
     * The three phases are named so the order reads as a sentence.
     * Retiring must happen BEFORE assigning -- otherwise a request that
     * was just served gets handed to a car and sends it to a floor
     * where nobody is waiting.
     */
    fun step() {
        retireCompletedRequests()
        assignUnservedRequests()
        advanceAllElevators()
    }

    private fun retireCompletedRequests() {
        pending.entries.removeAll { (request, elevator) ->
            elevator?.isServingFloor(request.floor) == true
        }
    }

    private fun assignUnservedRequests() {
        for (entry in pending.entries) {
            val assigned = entry.value

            // Already in the hands of a healthy car. Leave it alone.
            if (assigned != null && assigned.isAvailable()) continue

            // Either never assigned, or its car went out of service.
            // This single branch is the whole reassignment feature.
            val chosen = strategy.selectElevator(elevators, entry.key) ?: continue
            entry.setValue(chosen)
            chosen.addStop(entry.key.floor)
        }
    }

    private fun advanceAllElevators() {
        elevators.forEach { it.step() }
    }
}
