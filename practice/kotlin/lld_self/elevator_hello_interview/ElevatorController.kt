package lld_self.elevator_hello_interview

import lld_self.elevator_hello_interview.entities.Elevator
import lld_self.elevator_hello_interview.entities.HallDisplay
import lld_self.elevator_hello_interview.entities.HallPanel
import lld_self.elevator_hello_interview.entities.Request
import lld_self.elevator_hello_interview.entities.RequestType
import lld_self.elevator_hello_interview.strategies.ElevatorAssignmentStrategy
import lld_self.elevator_hello_interview.strategies.ElevatorMovementStrategy

class ElevatorController(
    val floorCount: Int = 10,
    val elevatorCount: Int = 3,
    val assignmentStrategy: ElevatorAssignmentStrategy,
    val movementStrategy: ElevatorMovementStrategy,
) {

    val elevators: List<Elevator> = List(elevatorCount) {
        Elevator(
            "elevator-${it + 1}",
            movementStrategy
        )
    }

    private val hallPanels = Array(floorCount) {
        HallPanel(
            it,
            this
        )
    }

    private val hallDisplays = Array(floorCount) {
        HallDisplay(
            it,
            elevators
        )
    }

    init {
        hallDisplays.forEach { display ->
            elevators.forEach { elevator ->
                elevator.addObserver(display)
            }
        }
    }


    fun getHallPanel(floor: Int): HallPanel {
        require(floor in 0..<floorCount) { "Invalid floor number" }
        return hallPanels[floor]
    }

    fun getHallDisplay(floor: Int): HallDisplay {
        require(floor in 0..<floorCount) { "Invalid floor number" }
        return hallDisplays[floor]
    }


    internal fun requestElevator(request: Request): Elevator {
        require(request.floor in 0..<floorCount) { "Invalid floor number" }
        require(request.type != RequestType.DESTINATION) { "Hall calls must be pickups" }

        val elevator = assignmentStrategy.pick(elevators, request)
        elevator.addRequest(request)
        return elevator
    }


    fun step() {
        elevators.forEach { it.step() }
    }

}
