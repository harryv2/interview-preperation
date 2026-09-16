package lld_self.parkinglot.entity

enum class SlotType {
    CAR,
    BIKE,
    TRUCK
}

object FitRules {

    private val preferences = mapOf(
        VehicleType.BIKE to listOf(SlotType.BIKE, SlotType.CAR, SlotType.TRUCK),
        VehicleType.CAR to listOf( SlotType.CAR, SlotType.TRUCK),
        VehicleType.TRUCK to listOf(SlotType.TRUCK),
    )

    fun spotsFor(vehicleType: VehicleType): List<SlotType> {
        return preferences[vehicleType] ?: emptyList()
    }
}

class Slot (
    val type: SlotType,
    var floorNumber: String
) {
    var currentVehicle: Vehicle? = null
        private set


    fun isFree(): Boolean = currentVehicle == null

    fun take(vehicle: Vehicle) {
        currentVehicle = vehicle
    }

    fun free() {
        currentVehicle = null
    }

}