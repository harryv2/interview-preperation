package lld.restaurantordering.entity


enum class LineStatus {
    PLACED,
    PREPARING,
    READY,
    SERVED,
    CANCELLED
}

class OrderLine(
    val id: String,
    val item: MenuItem,
    val quantity: Int,
    val note: String,
    val unitPrice: Money
) {

    var status: LineStatus = LineStatus.PLACED
        private set

    val amount: Money
        get() = unitPrice * quantity

    fun isBillable(): Boolean {
        return status != LineStatus.CANCELLED
    }

    fun isPending(): Boolean {
        return isBillable() && status != LineStatus.SERVED
    }

    fun moveTo(next: LineStatus) {
        check(next in ALLOWED.getValue(status)) { "${item.name} cannot go $status -> $next" }
        status = next
    }

    override fun toString(): String {
        val suffix = if (note.isEmpty()) "" else " ($note)"
        return "$quantity x ${item.name}$suffix $amount [$status]"
    }

    companion object {
        // cancel is only offered before the station starts, after that the food exists and the guest pays for it
        private val ALLOWED = mapOf(
            LineStatus.PLACED to setOf(LineStatus.PREPARING, LineStatus.CANCELLED),
            LineStatus.PREPARING to setOf(LineStatus.READY),
            LineStatus.READY to setOf(LineStatus.SERVED),
            LineStatus.SERVED to emptySet(),
            LineStatus.CANCELLED to emptySet()
        )
    }
}
