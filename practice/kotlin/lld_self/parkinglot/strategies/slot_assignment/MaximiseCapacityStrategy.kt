package lld_self.parkinglot.strategies.slot_assignment

import lld_self.parkinglot.entity.FitRules
import lld_self.parkinglot.entity.Floor
import lld_self.parkinglot.entity.Slot
import lld_self.parkinglot.entity.Vehicle


class MaximiseCapacityStrategy : SlotAssignmentStrategy {
    override fun getFreeSlot(
        floors: Array<Floor>,
        vehicle: Vehicle
    ): Slot? {
        var preferredType = FitRules.spotsFor(vehicle.type)

        for(type in preferredType) {
            for (floor in floors) {
                var slot = floor.reserve(type, vehicle)
                if (slot != null) {
                    return slot
                }
            }
        }

        return null
    }
}
