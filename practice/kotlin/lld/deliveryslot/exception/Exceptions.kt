package lld.deliveryslot.exception

class VanFullException(message: String) : RuntimeException(message)

class SlotNotAvailableException(message: String) : RuntimeException(message)

class NoServiceableWarehouseException(message: String) : RuntimeException(message)

class BookingNotFoundException(message: String) : RuntimeException(message)

class InvalidSlotException(message: String) : RuntimeException(message)
