package lld_self.vending_machine

import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.Money
import lld_self.vending_machine.entities.Product
import lld_self.vending_machine.entities.SlotId
import lld_self.vending_machine.entities.VendingMachine
import lld_self.vending_machine.entities.VendingSlot
import lld_self.vending_machine.strategies.GreedyChangeMaker

fun main() {

    val dove = Product("DV", "Dove", Money.rupees(40))
    val lux = Product("LX", "Lux", Money.rupees(20))
    val lifebuoy = Product("LF", "Lifebuoy", Money.rupees(10))

    val slotOne = VendingSlot(SlotId("S1"), dove, 20)
    val slotTwo = VendingSlot(SlotId("S1"), dove, 20)
    val slotThree = VendingSlot(SlotId("S1"), dove, 20)


    val initialBank = mapOf(
        Coin.ONE to 50,
        Coin.TWO to 50,
        Coin.FIVE to 50,
        Coin.TEN to 50,
        Coin.TWENTY to 50
    )

    val vendingMachine = VendingMachine(
        "v1",
        initialBank,
        listOf(slotOne, slotTwo, slotThree),
        GreedyChangeMaker()
    )


    vendingMachine.insertCoin(Coin.TWENTY)
    vendingMachine.insertCoin(Coin.TWENTY)
    vendingMachine.insertCoin(Coin.TEN)

    vendingMachine.selectSlot(slotOne.id)

    println(vendingMachine.dispense())
}