package lld.elevator

import lld.elevator.entity.Direction
import lld.elevator.service.Building

// =====================================================================
//  SIMULATION
// =====================================================================

/** Wall-clock pause between ticks, so the state changes are watchable. */
private const val TICK_DELAY_MILLIS = 400L

fun main() {
    val building = Building(floorCount = 10, elevatorCount = 3)

    val script: Map<Int, (Building) -> Unit> = mapOf(
        1 to { b -> b.floor(5).panel.pressUp() },
        2 to { b -> b.floor(2).panel.pressDown() },

        // Whoever gets picked up at 5 presses 9 from inside.
        12 to { b ->
            b.elevators.firstOrNull { it.isServingFloor(5) }?.panel?.pressFloor(9)
                ?: println("      (nobody at floor 5 yet)")
        },

        // Take a car out of service mid-run and watch it drain.
        14 to { b ->
            val busy = b.elevators.firstOrNull { it.hasStops() } ?: b.elevators[1]
            println("      >> maintenance requested on $busy")
            busy.setMaintenance(true)
        },

        // A new hall call while one car is draining.
        16 to { b -> b.floor(7).panel.pressDown() }
    )

    println("tick | " + building.elevators.joinToString("  ") { "E${it.id}".padEnd(24) } + "| pending")
    println("-".repeat(110))

    for (tick in 1..34) {
        script[tick]?.invoke(building)
        building.step()
        println(render(tick, building))
        Thread.sleep(TICK_DELAY_MILLIS)
    }
}

private fun render(tick: Int, building: Building): String {
    val cars = building.elevators.joinToString("  ") { e ->
        val arrow = when (e.direction()) {
            Direction.UP -> "↑"
            Direction.DOWN -> "↓"
            Direction.IDLE -> " "
        }
        val stops = if (e.hasStops()) e.pendingStops().toString() else "[]"
        "f${e.currentFloor()}$arrow ${e.state().name.take(7).padEnd(7)} ${e.doorState().name.take(5).padEnd(5)} $stops"
            .padEnd(24)
    }
    val pending = building.controller.pendingRequests()
    return "%4d | %s| %s".format(tick, cars, if (pending.isEmpty()) "-" else pending.toString())
}
