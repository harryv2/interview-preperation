package lld_self.vending_machine.exceptions

import lld_self.vending_machine.entities.Money
import lld_self.vending_machine.entities.SlotId

class ActionNotAllowedException(action: String, state: String) :
    RuntimeException("$action is not allowed while $state")

class SlotNotFoundException(slotId: SlotId) :
    RuntimeException("No slot ${slotId.id}")

class SlotEmptyException(slotId: SlotId) :
    RuntimeException("Slot ${slotId.id} is empty")

class InsufficientFundsException(price: Money, inserted: Money) :
    RuntimeException("Product costs $price, only $inserted inserted")

class ChangeUnavailableException(changeDue: Money) :
    RuntimeException("Can not return $changeDue in change")
