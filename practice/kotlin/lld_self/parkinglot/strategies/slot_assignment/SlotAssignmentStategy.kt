package lld_self.parkinglot.strategies.slot_assignment

import lld_self.parkinglot.entity.Floor
import lld_self.parkinglot.entity.Slot
import lld_self.parkinglot.entity.Vehicle


interface SlotAssignmentStrategy {
    fun getFreeSlot(floors: Array<Floor>, vehicle: Vehicle): Slot?
}