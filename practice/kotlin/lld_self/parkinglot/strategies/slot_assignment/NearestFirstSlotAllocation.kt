package lld_self.parkinglot.strategies.slot_assignment

import lld_self.parkinglot.entity.FitRules
import lld_self.parkinglot.entity.Floor
import lld_self.parkinglot.entity.Slot
import lld_self.parkinglot.entity.Vehicle


class NearestFirstSlotAllocation : SlotAssignmentStrategy {
    override fun getFreeSlot(
        floors: Array<Floor>,
        vehicle: Vehicle
    ): Slot? {
        var preferredType = FitRules.spotsFor(vehicle.type)

        var slot : Slot? = null;

        for(floor in floors) {
            for(type in preferredType) {
                slot = floor.reserve(type, vehicle)
                if(slot != null) {
                    break
                }
            }
        }

        return slot
    }
}