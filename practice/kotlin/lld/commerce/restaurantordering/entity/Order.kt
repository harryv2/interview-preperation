package lld.commerce.restaurantordering.entity

import java.time.Instant


enum class OrderStatus {
    OPEN,
    BILLED,
    CLOSED
}

class Order(
    val id: String,
    val tableNumber: String,
    val waiterId: String,
    val openedAt: Instant
) {

    private val lines = LinkedHashMap<String, OrderLine>()
    private val kots = mutableListOf<Kot>()

    var status: OrderStatus = OrderStatus.OPEN
        private set

    var bill: Bill? = null
        private set

    fun lines(): List<OrderLine> {
        return lines.values.toList()
    }

    fun kots(): List<Kot> {
        return kots.toList()
    }

    fun line(lineId: String): OrderLine {
        return requireNotNull(lines[lineId]) { "Order $id has no line $lineId" }
    }

    fun add(kot: Kot) {
        check(status == OrderStatus.OPEN) { "Order $id is $status" }
        kot.lines.forEach { lines[it.id] = it }
        kots.add(kot)
    }

    fun pendingLines(): List<OrderLine> {
        return lines.values.filter { it.isPending() }
    }

    fun subtotal(): Money {
        return lines.values
            .filter { it.isBillable() }
            .fold(Money.ZERO) { sum, line -> sum + line.amount }
    }

    fun attachBill(bill: Bill) {
        check(status == OrderStatus.OPEN) { "Order $id is already $status" }
        this.bill = bill
        status = OrderStatus.BILLED
    }

    // guests ask for the bill and then order one more coffee, so a printed bill is not final until it is settled
    fun reopen() {
        check(status == OrderStatus.BILLED) { "Order $id is $status" }
        bill = null
        status = OrderStatus.OPEN
    }

    fun close() {
        check(status == OrderStatus.BILLED) { "Order $id is $status, bill it first" }
        status = OrderStatus.CLOSED
    }

    override fun toString(): String {
        return "Order $id table $tableNumber by $waiterId $status"
    }
}
