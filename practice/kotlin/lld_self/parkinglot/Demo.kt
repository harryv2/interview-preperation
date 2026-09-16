package lld_self.parkinglot

import lld_self.parkinglot.entity.Floor
import lld_self.parkinglot.entity.ParkingLot
import lld_self.parkinglot.entity.Slot
import lld_self.parkinglot.entity.SlotType
import lld_self.parkinglot.entity.Vehicle
import lld_self.parkinglot.entity.VehicleType
import lld_self.parkinglot.strategies.payemnts.UPIMethod
import lld_self.parkinglot.strategies.pricing.FlatPricingStrategy
import lld_self.parkinglot.strategies.slot_assignment.NearestFirstSlotAllocation

fun main() {

    var floorSlotsMap = mapOf<String, List<SlotType>>(
        "Floor-1" to List(5){ SlotType.TRUCK} + List(5){ SlotType.CAR} + List(5){ SlotType.BIKE},
        "Floor-2" to List(10){ SlotType.TRUCK} + List(20){ SlotType.CAR} + List(30){ SlotType.BIKE},
        "Floor-3" to  List(20){ SlotType.CAR} + List(30){ SlotType.BIKE}
    )

    var floors = mutableListOf<Floor>();

    floorSlotsMap.forEach { (key, slotTypes) ->
        var slots = slotTypes.map { it ->
            Slot(it, key)
        }

        floors.add(Floor(number = key, slots.toTypedArray()))
    }

    var lot = ParkingLot(
        floors = floors.toTypedArray(),
        slotStrategy = NearestFirstSlotAllocation(),
        pricingStrategy = FlatPricingStrategy()
    )


    var entry1Gate = lot.EntryGate("ENTRY-GATE-1")
    var entry2Gate = lot.EntryGate("ENTRY-GATE-2")

    var car1 = Vehicle(VehicleType.CAR,"abc")
    var bike1 = Vehicle(VehicleType.BIKE,"def")
    var truck1 = Vehicle(VehicleType.TRUCK,"dsds")


    entry1Gate.park(car1)
    entry2Gate.park(bike1)
//    entry2Gate.park(car1)
    entry1Gate.park(truck1)


    var exitGate = lot.ExitGate("EXIT-1")

    exitGate.quote(car1)
    exitGate.unpark(car1, UPIMethod())
}