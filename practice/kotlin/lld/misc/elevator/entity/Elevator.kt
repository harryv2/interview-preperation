package lld.misc.elevator.entity

import lld.misc.elevator.strategy.LookStrategy
import lld.misc.elevator.strategy.MovementStrategy
import java.util.Collections
import java.util.NavigableSet
import java.util.TreeSet

// =====================================================================
//  ELEVATOR
//
//  Fields are private. Everything outside reads through methods, so
//  the class can change how it stores things without breaking callers.
//
//  Deliberately absent: distanceTo(floor). Distance means different
//  things to different scheduling strategies, so the elevator exposes
//  raw facts and each strategy does its own arithmetic.
//
//  GOING_OUT_OF_SERVICE exists because taking a car out of service is
//  not instant. It has to finish carrying whoever is inside first.
//  Any state change that takes time needs a state for the in-between.
// =====================================================================

enum class ElevatorState { IDLE, MOVING, STOPPED, GOING_OUT_OF_SERVICE, MAINTENANCE }

class Elevator(
    val id: Int,
    private val movement: MovementStrategy = LookStrategy(),
    private val door: Door = Door(),
    private val display: Display = Display(),
    startFloor: Int = 0
) {
    private var currentFloor: Int = startFloor
    private var direction: Direction = Direction.IDLE
    private var state: ElevatorState = ElevatorState.IDLE
    private val stops = TreeSet<Int>()

    /** Created lazily because the panel needs a reference back to this elevator. */
    val panel: ElevatorPanel by lazy { ElevatorPanel(this) }

    // ---------- queries ----------
    // These exist so callers can ask questions instead of reading
    // fields and doing the reasoning themselves.

    fun currentFloor(): Int = currentFloor

    fun direction(): Direction = direction

    fun state(): ElevatorState = state

    /**
     * One method, not two enum comparisons at every call site. Add a
     * new out-of-service reason later and no caller changes.
     */
    fun isAvailable(): Boolean =
        state != ElevatorState.MAINTENANCE && state != ElevatorState.GOING_OUT_OF_SERVICE

    fun hasStops(): Boolean = stops.isNotEmpty()

    fun hasStop(floor: Int): Boolean = floor in stops

    fun pendingStopCount(): Int = stops.size

    /** The controller asks this instead of reading floor and door state itself. */
    fun isServingFloor(floor: Int): Boolean =
        currentFloor == floor && door.state == DoorState.OPEN

    /** For displays and logging only. */
    fun doorState(): DoorState = door.state

    fun pendingStops(): List<Int> = stops.toList()

    // ---------- commands ----------

    fun addStop(floor: Int) {
        if (!isAvailable()) return

        // Already standing here with the doors usable. Just open up.
        if (floor == currentFloor && state != ElevatorState.MOVING) {
            openDoor()
            return
        }

        stops.add(floor)

        // Parked, so this request is what decides which way we go.
        // This is the ONLY moment a target sets the direction.
        // Every other moment, direction picks the target.
        if (direction == Direction.IDLE) {
            direction = if (floor > currentFloor) Direction.UP else Direction.DOWN
        }
    }

    /** The guard is the point of this method, not the delegation. */
    fun openDoor() {
        if (state == ElevatorState.MOVING) return
        door.open()
    }

    fun closeDoor() {
        if (state == ElevatorState.MOVING) return
        door.close()
    }

    /**
     * Turning maintenance ON does not freeze the car. It stops new work
     * arriving (isAvailable goes false immediately) and lets the car
     * finish delivering whoever is inside. `stops` is untouched --
     * the controller reassigns the waiting hall people on its own.
     */
    fun setMaintenance(on: Boolean) {
        if (!on) {
            state = ElevatorState.IDLE
            return
        }
        if (state == ElevatorState.MAINTENANCE ||
            state == ElevatorState.GOING_OUT_OF_SERVICE
        ) return

        state = ElevatorState.GOING_OUT_OF_SERVICE
    }

    /** One tick of time. This is the engine of the whole design. */
    fun step() {
        // Frozen. The door may still be mid-cycle, so let it finish,
        // but never move.
        if (state == ElevatorState.MAINTENANCE) {
            door.step()
            return
        }

        // Cannot move with the door doing anything but sitting closed.
        if (!door.isClosed()) {
            door.step()
            return
        }

        if (stops.isEmpty()) {
            finishOrIdle()
            return
        }

        val next = movement.nextStop(currentFloor, direction, unmodifiableStops())

        // Nothing left this way. Turn around and move on the next tick.
        if (next == null) {
            direction =
                if (direction == Direction.IDLE) {
                    if (stops.first() > currentFloor) Direction.UP else Direction.DOWN
                } else {
                    direction.opposite()
                }
            return
        }

        if (next == currentFloor) {
            stops.remove(currentFloor)
            state = ElevatorState.STOPPED
            openDoor()
            return
        }

        currentFloor += if (direction == Direction.UP) 1 else -1
        state = ElevatorState.MOVING
        display.show(currentFloor, direction)
    }

    private fun finishOrIdle() {
        direction = Direction.IDLE
        if (state == ElevatorState.GOING_OUT_OF_SERVICE) {
            // ORDER MATTERS. Set MAINTENANCE first and the door never
            // opens, because the next tick hits the frozen guard above.
            door.open()
            state = ElevatorState.MAINTENANCE
        } else {
            state = ElevatorState.IDLE
        }
        display.show(currentFloor, Direction.IDLE)
    }

    /** Strategies get to read the stops, not edit them. */
    private fun unmodifiableStops(): NavigableSet<Int> =
        Collections.unmodifiableNavigableSet(stops)

    override fun toString(): String = "E$id"
}
