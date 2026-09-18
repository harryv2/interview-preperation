package lld_self.elevator

import lld_self.elevator.entities.Direction
import lld_self.elevator.entities.Elevator
import lld_self.elevator.entities.HallPanel
import lld_self.elevator.strategies.ElevatorAssignmentStrategy
import lld_self.elevator.strategies.ElevatorMovementStrategy

class ElevatorController(
    val floorCount: Int = 10,
    val elevatorCount: Int = 3,
    val assignmentStrategy: ElevatorAssignmentStrategy,
    val movementStrategy: ElevatorMovementStrategy,
) {

    var elevators = mutableListOf<Elevator>()
    private val hallPanels = Array(floorCount) {
        HallPanel(
            it,
            this
        )
    }

    init {
        for (i in 1..elevatorCount) {
            elevators.add(
                Elevator(
                    "elevator-$i",
                    movementStrategy
                )
            )
        }
    }


    fun getHallPanel(floor: Int): HallPanel {
        require(floor in 0..<floorCount) { "Invalid floor number" }
        return hallPanels[floor]
    }


    internal fun handleHallRequest(floor: Int, direction: Direction): Elevator {
        var elevator = assignmentStrategy.pick(elevators, floor, direction)
        elevator.addStop(floor)
        return elevator
    }


    fun step() {
        elevators.forEach { it.step() }
    }

}