package lld_self.elevator

import lld_self.elevator.entities.Direction
import lld_self.elevator.entities.Elevator
import lld_self.elevator.entities.HallDisplay
import lld_self.elevator.entities.HallPanel
import lld_self.elevator.strategies.ElevatorAssignmentStrategy
import lld_self.elevator.strategies.ElevatorMovementStrategy

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


    internal fun handleHallRequest(floor: Int, direction: Direction): Elevator {
        var elevator = assignmentStrategy.pick(elevators, floor, direction)
        elevator.addStop(floor, direction)
        return elevator
    }


    fun step() {
        elevators.forEach { it.step() }
    }

}
