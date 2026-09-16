package lld_self.parkinglot.entity

import lld_self.parkinglot.strategies.payemnts.PaymentMethod
import lld_self.parkinglot.strategies.payemnts.PaymentMethodStrategy
import lld_self.parkinglot.strategies.pricing.PricingStrategy
import lld_self.parkinglot.strategies.slot_assignment.SlotAssignmentStrategy
import kotlin.time.Clock
import kotlin.uuid.Uuid


enum class ParkResultStatus {
    SUCCESS,
    NO_FREE_SLOTS
}

data class ParkResult(
    val status: ParkResultStatus,
    val ticket: Ticket?
)

class ParkingLot(
    var floors: Array<Floor>,
    var slotStrategy: SlotAssignmentStrategy,
    var pricingStrategy: PricingStrategy
) {

    private var floorNumberMap = hashMapOf<String, Floor>()
    private var parkedPlates = hashMapOf<String, Uuid>()
    private var issuedActiveTickets = hashMapOf<Uuid, Ticket>()


    init {
        floors.forEach {
            floorNumberMap.putIfAbsent(it.number, it)
        }
    }

    fun EntryGate(id: String): EntryGate {
        return EntryGate(id, this)
    }

    fun ExitGate(id: String): ExitGate {
        return ExitGate(id, this)
    }


    internal fun park(vehicle: Vehicle, gateId: String): ParkResult {
        require(!parkedPlates.contains(vehicle.licensePlate)) {
            "Vehicle with ${vehicle.licensePlate} already parked"
        }

        val slot = slotStrategy.getFreeSlot(floors, vehicle) ?: return ParkResult(ParkResultStatus.NO_FREE_SLOTS, null)

        val ticket = Ticket(
            id = Uuid.random(),
            Clock.System.now(),
            vehicle,
            slot,
            gateId
        )

        issuedActiveTickets[ticket.id] = ticket
        parkedPlates[vehicle.licensePlate] = ticket.id

        return ParkResult(ParkResultStatus.SUCCESS, ticket)
    }


    internal fun getFare(vehicle: Vehicle): Money {
        require(parkedPlates.contains(vehicle.licensePlate)) {"Vehicle ${vehicle.licensePlate} not found in system"}
        var ticketId = parkedPlates[vehicle.licensePlate]
        var ticket = issuedActiveTickets[ticketId]!!

        return pricingStrategy.getPrice(ticket)
    }

    internal fun makePayment(vehicle: Vehicle, paymentMethod: PaymentMethodStrategy): Ticket {
        require(parkedPlates.contains(vehicle.licensePlate)) {"Vehicle ${vehicle.licensePlate} not found in system"}
        var ticketId = parkedPlates[vehicle.licensePlate]

        var ticket = issuedActiveTickets[ticketId]!!

        var payment = paymentMethod.pay(pricingStrategy.getPrice(ticket))

        ticket.markPaid(payment)

        var floor = floorNumberMap[ticket.slot.floorNumber]!!
        floor.markFree(ticket.slot)

        parkedPlates.remove(ticket.vehicle.licensePlate)
        issuedActiveTickets.remove(ticket.id)

        return ticket
    }
}


class EntryGate(val id: String, private val lot: ParkingLot) {
    fun park(vehicle: Vehicle): ParkResult {
        val result = lot.park(vehicle, id)
        return result
    }
}

class ExitGate(val id: String, private val lot: ParkingLot) {

    fun quote(vehicle: Vehicle): Money = lot.getFare(vehicle)

    fun unpark(vehicle: Vehicle, mode: PaymentMethodStrategy): Ticket {
        val ticket = lot.makePayment(vehicle, mode)
        return ticket
    }
}