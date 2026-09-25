package lld.fintech.cryptowallet

import lld.fintech.cryptowallet.entity.Amount
import lld.fintech.cryptowallet.entity.Currency
import lld.fintech.cryptowallet.service.WalletService

fun main() {

    val service = WalletService()

    val asha = service.openWallet("asha")
    val ravi = service.openWallet("ravi")

    banner("deposits")
    println("  ${service.deposit(asha.id, Amount.of(2, Currency.BTC))}")
    println("  ${service.deposit(asha.id, Amount.of(50000, Currency.INR))}")
    println("  ${service.deposit(ravi.id, Amount.of(1, Currency.BTC))}")
    println("  $asha")
    println("  $ravi")

    banner("peer to peer transfer")
    println("  ${service.transfer(asha.id, ravi.id, Amount.of(1, Currency.BTC))}")
    println("  asha ${asha.balanceOf(Currency.BTC)}, ravi ${ravi.balanceOf(Currency.BTC)}")
    println("  asha's INR is untouched: ${asha.balanceOf(Currency.INR)}")

    banner("a retry with the same key does not move money twice")
    val key = "req-42"
    val first = service.transfer(asha.id, ravi.id, Amount.of(1, Currency.BTC), key)
    val retry = service.transfer(asha.id, ravi.id, Amount.of(1, Currency.BTC), key)
    println("  first ${first.id}, retry ${retry.id}, same transaction: ${first.id == retry.id}")
    println("  ravi holds ${ravi.balanceOf(Currency.BTC)}, not 4")

    banner("overdraft is refused and nothing moves")
    val before = asha.balanceOf(Currency.BTC)
    attempt("asha sends 10 BTC holding $before") {
        service.transfer(asha.id, ravi.id, Amount.of(10, Currency.BTC))
    }
    println("  asha still holds ${asha.balanceOf(Currency.BTC)}")

    banner("ledger reconstructs the balance")
    service.historyOf(ravi.id).forEach { println("  $it") }

    banner("rejected")
    attempt("mix currencies") {
        Amount.of(1, Currency.BTC) + Amount.of(1, Currency.INR)
    }
    attempt("a negative amount") {
        Amount(-5, Currency.BTC)
    }
    attempt("send to yourself") {
        service.transfer(asha.id, asha.id, Amount.of(1, Currency.BTC))
    }
    attempt("withdraw a currency you do not hold") {
        service.withdraw(ravi.id, Amount.of(100, Currency.USDT))
    }
    attempt("unknown wallet") {
        service.deposit("W-nope", Amount.of(1, Currency.BTC))
    }

    banner("total BTC in the system")
    println("  ${service.totalHeld(Currency.BTC)}")
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
