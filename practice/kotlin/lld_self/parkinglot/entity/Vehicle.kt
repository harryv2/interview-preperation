package lld_self.parkinglot.entity

enum class VehicleType {
    CAR,
    BIKE,
    TRUCK
}


class Vehicle(
    var type: VehicleType,
    var licensePlate: String
) {


    override fun toString(): String {
        return "$type - $licensePlate"
    }
}