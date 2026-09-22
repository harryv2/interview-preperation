package lld_self.vending_machine

import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.Money
import lld_self.vending_machine.entities.Product
import lld_self.vending_machine.entities.SlotId
import lld_self.vending_machine.entities.VendingSlot
import lld_self.vending_machine.strategies.GreedyChangeMaker

fun main() {

    val dove = Product("DV", "Dove", Money.rupees(40))
    val lux = Product("LX", "Lux", Money.rupees(20))
    val lifebuoy = Product("LF", "Lifebuoy", Money.rupees(10))

    val slotOne = VendingSlot(SlotId("S1"), dove, 2)
    val slotTwo = VendingSlot(SlotId("S2"), lux, 1)
    val slotThree = VendingSlot(SlotId("S3"), lifebuoy, 1)

    val initialBank = mapOf(
        Coin.ONE to 5,
        Coin.TWO to 5,
        Coin.FIVE to 5,
        Coin.TEN to 5,
        Coin.TWENTY to 5
    )

    val vendingMachine = VendingMachine(
        "v1",
        initialBank,
        listOf(slotOne, slotTwo, slotThree),
        GreedyChangeMaker()
    )

    banner("buy Dove with change due")
    insert(vendingMachine, Coin.TWENTY, Coin.TWENTY, Coin.TEN)
    vendingMachine.selectSlot(slotOne.id)
    println("  ${vendingMachine.dispense()}")

    banner("buy Lifebuoy with exact money")
    insert(vendingMachine, Coin.TEN)
    vendingMachine.selectSlot(slotThree.id)
    println("  ${vendingMachine.dispense()}")

    banner("not enough money")
    insert(vendingMachine, Coin.FIVE)
    attempt("select Lux for Rs 5") {
        vendingMachine.selectSlot(slotTwo.id)
    }
    println("  refund ${vendingMachine.cancel()}")

    banner("change cannot be made")
    val santoor = Product("ST", "Santoor", Money.rupees(15))
    val brokeMachine = VendingMachine("v2", emptyMap(), listOf(VendingSlot(SlotId("S1"), santoor, 1)), GreedyChangeMaker())
    insert(brokeMachine, Coin.TWENTY)
    attempt("select Rs 15 item with Rs 20 and an empty float") {
        brokeMachine.selectSlot(SlotId("S1"))
    }
    println("  refund ${brokeMachine.cancel()}")

    banner("sell out then restock")
    insert(vendingMachine, Coin.TWENTY, Coin.TWENTY)
    vendingMachine.selectSlot(slotOne.id)
    println("  ${vendingMachine.dispense()}")
    insert(vendingMachine, Coin.TWENTY)
    vendingMachine.selectSlot(slotTwo.id)
    println("  ${vendingMachine.dispense()}")
    attempt("insert a coin once every slot is empty") {
        vendingMachine.insertCoin(Coin.TEN)
    }

    vendingMachine.restock(mapOf(slotOne.id to 5, slotTwo.id to 5, slotThree.id to 5))
    println("  restocked")
    insert(vendingMachine, Coin.TWENTY, Coin.TWENTY)
    vendingMachine.selectSlot(slotOne.id)
    println("  ${vendingMachine.dispense()}")

    banner("wrong order of operations")
    attempt("dispense without selecting") {
        vendingMachine.dispense()
    }
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun insert(machine: VendingMachine, vararg coins: Coin) {
    coins.forEach { machine.insertCoin(it) }
    println("  inserted ${coins.toList()}, showing ${machine.insertedAmount()}")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
