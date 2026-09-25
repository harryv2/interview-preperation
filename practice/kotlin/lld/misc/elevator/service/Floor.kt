package lld.misc.elevator.service

import lld.misc.elevator.entity.Display

class Floor(val number: Int, controller: ElevatorController) {
    val panel: HallPanel = HallPanel(number, controller)
    val display: Display = Display()
}
