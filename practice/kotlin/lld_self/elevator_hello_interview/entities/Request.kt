package lld_self.elevator_hello_interview.entities

enum class RequestType {
    PICKUP_UP,
    PICKUP_DOWN,
    DESTINATION
}

data class Request(
    val floor: Int,
    val type: RequestType
) {
    val direction: Direction
        get() = when (type) {
            RequestType.PICKUP_UP -> {
                Direction.UP
            }
            RequestType.PICKUP_DOWN -> {
                Direction.DOWN
            }
            RequestType.DESTINATION -> {
                Direction.IDLE
            }
        }

    fun servesDirection(direction: Direction): Boolean {
        return when (type) {
            RequestType.PICKUP_UP -> {
                direction == Direction.UP
            }
            RequestType.PICKUP_DOWN -> {
                direction == Direction.DOWN
            }
            RequestType.DESTINATION -> {
                true
            }
        }
    }

    companion object {
        fun pickup(floor: Int, direction: Direction): Request {
            require(direction != Direction.IDLE) { "Pickup direction must be UP or DOWN" }
            val type = if (direction == Direction.UP) RequestType.PICKUP_UP else RequestType.PICKUP_DOWN
            return Request(floor, type)
        }

        fun destination(floor: Int): Request {
            return Request(floor, RequestType.DESTINATION)
        }
    }
}
