package lld_self.parkinglot.entity

import lld_self.parkinglot.strategies.payemnts.PaymentMethod
import lld_self.parkinglot.strategies.payemnts.PaymentMethodStrategy
import lld_self.parkinglot.strategies.pricing.PricingStrategy
import lld_self.parkinglot.strategies.slot_assignment.SlotAssignmentStrategy
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.uuid.Uuid


enum class ParkResultStatus {
    SUCCESS,
    NO_FREE_SLOTS,
    ALREADY_PARKED
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
    private var parkedPlates = ConcurrentHashMap<String, Uuid>()
    private var issuedActiveTickets = ConcurrentHashMap<Uuid, Ticket>()


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
        var ticketId = Uuid.random()

        // claim the plate first, so two gates can't park the same vehicle twice
        if (parkedPlates.putIfAbsent(vehicle.licensePlate, ticketId) != null) {
            return ParkResult(ParkResultStatus.ALREADY_PARKED, null)
        }

        val slot = slotStrategy.getFreeSlot(floors, vehicle)
        if (slot == null) {
            parkedPlates.remove(vehicle.licensePlate)
            return ParkResult(ParkResultStatus.NO_FREE_SLOTS, null)
        }

        val ticket = Ticket(
            id = ticketId,
            Clock.System.now(),
            vehicle,
            slot,
            gateId
        )

        issuedActiveTickets[ticket.id] = ticket

        return ParkResult(ParkResultStatus.SUCCESS, ticket)
    }


    internal fun getFare(vehicle: Vehicle): Money {
        var ticket = findActiveTicket(vehicle)
        return stampExit(ticket)
    }

    internal fun makePayment(vehicle: Vehicle, paymentMethod: PaymentMethodStrategy): Ticket {
        var ticket = findActiveTicket(vehicle)
        var fare = stampExit(ticket)

        var payment = paymentMethod.pay(fare)
        ticket.markPaid(payment)

        // whoever removes the plate owns the exit, so the slot can't be freed twice
        if (parkedPlates.remove(ticket.vehicle.licensePlate, ticket.id)) {
            issuedActiveTickets.remove(ticket.id)

            var floor = floorNumberMap[ticket.slot.floorNumber]!!
            floor.markFree(ticket.slot)
        }

        return ticket
    }

    private fun findActiveTicket(vehicle: Vehicle): Ticket {
        var ticketId = parkedPlates[vehicle.licensePlate]
        var ticket = ticketId?.let { issuedActiveTickets[it] }

        return requireNotNull(ticket) {"Vehicle ${vehicle.licensePlate} not found in system"}
    }

    // exit time and fare are stamped once, so quote and charge always agree
    private fun stampExit(ticket: Ticket): Money {
        if (ticket.fare == null) {
            var now = Clock.System.now()
            ticket.markExit(now, pricingStrategy.getPrice(ticket, now))
        }

        return ticket.fare!!
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
