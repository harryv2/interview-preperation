package lld.commerce.restaurantordering.service

import lld.commerce.restaurantordering.entity.Bill
import lld.commerce.restaurantordering.entity.DiningTable
import lld.commerce.restaurantordering.entity.Kot
import lld.commerce.restaurantordering.entity.LineStatus
import lld.commerce.restaurantordering.entity.Menu
import lld.commerce.restaurantordering.entity.Money
import lld.commerce.restaurantordering.entity.Order
import lld.commerce.restaurantordering.entity.OrderLine
import lld.commerce.restaurantordering.entity.OrderStatus
import lld.commerce.restaurantordering.entity.Station
import lld.commerce.restaurantordering.entity.TableStatus
import lld.commerce.restaurantordering.strategy.BillPolicy
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


data class LineRequest(
    val menuItemId: String,
    val quantity: Int,
    val note: String = ""
)

class RestaurantService(
    tables: List<DiningTable>,
    val menu: Menu,
    private val billPolicy: BillPolicy
) {

    private val lock = ReentrantLock()
    private val tablesByNumber = tables.associateBy { it.number }
    private val orders = HashMap<String, Order>()
    private val kotsById = HashMap<String, Kot>()
    private val stationQueues = Station.values().associateWith { ArrayDeque<Kot>() }
    private val punches = HashMap<String, List<Kot>>()

    fun freeTables(): List<DiningTable> {
        lock.withLock {
            return tablesByNumber.values.filter { it.status == TableStatus.FREE }
        }
    }

    fun openTable(tableNumber: String, waiterId: String): Order {
        lock.withLock {
            val table = table(tableNumber)
            val order = Order(newId(), tableNumber, waiterId, Instant.now())

            table.occupy(order.id)
            orders[order.id] = order
            return order
        }
    }

    // one punch splits into one ticket per station, so the bar pours while the kitchen is still cooking
    fun punch(orderId: String, punchId: String, requests: List<LineRequest>): List<Kot> {
        lock.withLock {
            // a device that never saw the ack resends the same punchId, it must not cook the food twice
            punches[punchId]?.let { return it }

            require(requests.isNotEmpty()) { "Nothing to punch" }

            val order = order(orderId)
            check(order.status != OrderStatus.CLOSED) { "Order $orderId is closed" }

            if (order.status == OrderStatus.BILLED) {
                order.reopen()
            }

            val lines = requests.map {
                val item = menu.item(it.menuItemId)
                require(item.available) { "${item.name} is off the menu right now" }
                require(it.quantity > 0) { "Quantity for ${item.name} must be positive" }

                // price is copied onto the line, a menu reprice mid meal must not move the bill
                OrderLine(newId(), item, it.quantity, it.note, item.price)
            }

            val kots = lines.groupBy { it.item.station }.map { (station, stationLines) ->
                Kot(newId(), order.id, order.tableNumber, station, stationLines, Instant.now())
            }

            kots.forEach {
                order.add(it)
                kotsById[it.id] = it
                stationQueues.getValue(it.station).addLast(it)
            }

            punches[punchId] = kots
            return kots
        }
    }

    fun cancelLine(orderId: String, lineId: String) {
        lock.withLock {
            order(orderId).line(lineId).moveTo(LineStatus.CANCELLED)
        }
    }

    fun pendingKots(station: Station): List<Kot> {
        lock.withLock {
            return stationQueues.getValue(station).filter { !it.isClosed() }
        }
    }

    fun startPreparing(kotId: String) {
        lock.withLock {
            kot(kotId).lines
                .filter { it.status == LineStatus.PLACED }
                .forEach { it.moveTo(LineStatus.PREPARING) }
        }
    }

    fun markReady(kotId: String) {
        lock.withLock {
            kot(kotId).lines
                .filter { it.status == LineStatus.PREPARING }
                .forEach { it.moveTo(LineStatus.READY) }
        }
    }

    fun markServed(orderId: String, lineId: String) {
        lock.withLock {
            order(orderId).line(lineId).moveTo(LineStatus.SERVED)
        }
    }

    fun generateBill(orderId: String): Bill {
        lock.withLock {
            val order = order(orderId)
            val pending = order.pendingLines()
            check(pending.isEmpty()) { "Still on the pass: ${pending.joinToString { it.item.name }}" }

            val bill = billPolicy.bill(order)
            order.attachBill(bill)
            return bill
        }
    }

    // the table is released only after the money is in, same as the bill is printed only after the food is served
    fun settle(orderId: String, paid: Money): Order {
        lock.withLock {
            val order = order(orderId)
            val bill = requireNotNull(order.bill) { "Order $orderId has no bill" }
            require(paid >= bill.total) { "Paid $paid against a bill of ${bill.total}" }

            order.close()
            table(order.tableNumber).free()
            return order
        }
    }

    fun order(orderId: String): Order {
        return requireNotNull(orders[orderId]) { "No order $orderId" }
    }

    private fun table(tableNumber: String): DiningTable {
        return requireNotNull(tablesByNumber[tableNumber]) { "No table $tableNumber" }
    }

    private fun kot(kotId: String): Kot {
        return requireNotNull(kotsById[kotId]) { "No kot $kotId" }
    }

    private fun newId(): String {
        return UUID.randomUUID().toString().take(6)
    }
}
