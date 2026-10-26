package lld_self.elevator_hello_interview

import lld_self.elevator_hello_interview.entities.Direction
import lld_self.elevator_hello_interview.entities.DoorState
import lld_self.elevator_hello_interview.entities.Elevator
import lld_self.elevator_hello_interview.entities.ElevatorObserver
import lld_self.elevator_hello_interview.entities.Request
import lld_self.elevator_hello_interview.entities.RequestType
import lld_self.elevator_hello_interview.strategies.CommittedIdleNearestAssignment
import lld_self.elevator_hello_interview.strategies.LookMovementStrategy

// one elevator so the direction-aware stops are easy to follow:
// going up it skips the down-call at 4, stops at 6 for the up-call, then serves 4 on the way down
fun main() {

    val controller = ElevatorController(
        floorCount = 10,
        elevatorCount = 1,
        assignmentStrategy = CommittedIdleNearestAssignment(),
        movementStrategy = LookMovementStrategy(),
    )
    val elevator = controller.elevators.first()

    val pressWhenDoorOpensAt = mutableMapOf(
        6 to 9,
        4 to 0,
    )

    elevator.addObserver(object : ElevatorObserver {
        override fun onDoorStateChanged(elevator: Elevator, doorState: DoorState) {
            if (doorState != DoorState.OPEN) {
                return
            }
            pressWhenDoorOpensAt.remove(elevator.currentFloor)?.let { elevator.elevatorPanel.pressFloor(it) }
        }
    })

    val script: Map<Int, () -> Unit> = mapOf(
        1 to { elevator.elevatorPanel.pressFloor(8) },
        2 to { controller.getHallPanel(4).callLift(Direction.DOWN) },
        3 to { controller.getHallPanel(6).callLift(Direction.UP) },
    )

    for (tick in 1..110) {
        script[tick]?.invoke()
        controller.step()
        println(render(tick, controller.elevators))
        Thread.sleep(50)
    }
}

private fun render(tick: Int, elevators: List<Elevator>): String {
    val columns = elevators.joinToString(" | ") {
        "${it.name} [${it.display.text.padEnd(4)}] ${it.state.name.padEnd(7)} door=${it.door.state.name.padEnd(7)} requests=${renderRequests(it.pendingRequests)}"
    }
    return "t=${tick.toString().padStart(3, '0')} | $columns"
}

private fun renderRequests(requests: Set<Request>): String {
    return requests
        .sortedBy { it.floor }
        .joinToString(",", "[", "]") {
            val marker = when (it.type) {
                RequestType.PICKUP_UP -> "^"
                RequestType.PICKUP_DOWN -> "v"
                RequestType.DESTINATION -> "*"
            }
            "${it.floor}$marker"
        }
}
