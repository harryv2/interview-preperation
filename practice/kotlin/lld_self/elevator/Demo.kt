package lld_self.elevator

import lld_self.elevator.entities.Direction
import lld_self.elevator.entities.DoorState
import lld_self.elevator.entities.Elevator
import lld_self.elevator.strategies.AssignNearestInThatDirection
import lld_self.elevator.strategies.LookMovementStrategy

fun main() {

    val controller = ElevatorController(
        floorCount = 10,
        elevatorCount = 3,
        assignmentStrategy = AssignNearestInThatDirection(),
        movementStrategy = LookMovementStrategy(),
    )

    fun elevatorOpenAt(floor: Int): Elevator {
        return controller.elevators.first {
            it.currentFloor == floor && it.door.state == DoorState.OPEN
        }
    }

    val script: Map<Int, () -> Unit> = mapOf(
        1 to { controller.getHallPanel(3).callLift(Direction.DOWN) },
        5 to { controller.getHallPanel(7).callLift(Direction.DOWN) },
        8 to { elevatorOpenAt(3).elevatorPanel.pressFloor(0) },
        16 to { elevatorOpenAt(7).elevatorPanel.pressFloor(2) },
        29 to { controller.getHallPanel(5).callLift(Direction.DOWN) },
    )

    for (tick in 1..50) {
        script[tick]?.invoke()
        controller.step()
        println(render(tick, controller.elevators))
        Thread.sleep(100)
    }

    println()
    println("hall display at floor 0 -> ${controller.getHallDisplay(0).render()}")
}

private fun render(tick: Int, elevators: List<Elevator>): String {
    val columns = elevators.joinToString(" | ") {
        "${it.name} [${it.display.text.padEnd(4)}] ${it.state.name.padEnd(7)} door=${it.door.state.name.padEnd(7)}"
    }
    return "t=${tick.toString().padStart(2, '0')} | $columns"
}
