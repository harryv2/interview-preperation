package lld.commerce.amazonlocker.machine

import lld.commerce.amazonlocker.entity.Reservation
import kotlin.uuid.Uuid

sealed class MachineState {
    val name: String
        get() = this::class.simpleName!!

    open fun scanPackage(machine: LockerMachine, packageId: Uuid): MachineState {
        machine.display("Please wait, machine is $name")
        return this
    }

    open fun enterCode(machine: LockerMachine, code: String): MachineState {
        machine.display("Please wait, machine is $name")
        return this
    }

    open fun doorClosed(machine: LockerMachine): MachineState {
        return this
    }

    open fun timeout(machine: LockerMachine): MachineState {
        return this
    }

    open fun reset(machine: LockerMachine): MachineState {
        return this
    }
}

object Idle : MachineState() {
    override fun scanPackage(machine: LockerMachine, packageId: Uuid): MachineState {
        val reservation = machine.backend.findForDrop(machine.locker.id, packageId)
        if (reservation == null) {
            machine.display("No reservation for this package at this locker")
            return this
        }

        machine.openDoor(reservation.slot)
        return DoorOpen(reservation)
    }

    override fun enterCode(machine: LockerMachine, code: String): MachineState {
        val reservation = machine.backend.findForPickup(machine.locker.id, code)
        if (reservation == null) {
            machine.display("Invalid or expired code")
            return this
        }

        machine.openDoor(reservation.slot)
        return DoorOpen(reservation)
    }
}

class DoorOpen(private val reservation: Reservation) : MachineState() {
    override fun doorClosed(machine: LockerMachine): MachineState {
        machine.closeDoor(reservation.slot)
        machine.backend.doorClosed(reservation)
        return Idle
    }

    override fun timeout(machine: LockerMachine): MachineState {
        machine.display("Door ${reservation.slot.id} left open, alerting staff")
        return Alert(reservation)
    }
}

class Alert(private val reservation: Reservation) : MachineState() {
    override fun doorClosed(machine: LockerMachine): MachineState {
        machine.closeDoor(reservation.slot)
        machine.backend.doorClosed(reservation)
        return Idle
    }

    override fun reset(machine: LockerMachine): MachineState {
        machine.closeDoor(reservation.slot)
        return Idle
    }
}

object OutOfService : MachineState() {
    override fun reset(machine: LockerMachine): MachineState {
        return Idle
    }
}
