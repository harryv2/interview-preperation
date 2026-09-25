package lld.fintech.cryptowallet.entity

import java.util.concurrent.locks.ReentrantLock


class InsufficientBalanceException(walletId: String, wanted: Amount, held: Amount) :
    RuntimeException("Wallet $walletId holds $held, needs $wanted")


class Wallet(
    val id: String,
    val userId: String
) {

    val lock = ReentrantLock()
    private val balances = HashMap<Currency, Long>()

    fun balanceOf(currency: Currency): Amount {
        lock.lock()
        try {
            return Amount(balances[currency] ?: 0, currency)
        } finally {
            lock.unlock()
        }
    }

    fun allBalances(): Map<Currency, Amount> {
        lock.lock()
        try {
            return balances
                .filterValues { it > 0 }
                .mapValues { Amount(it.value, it.key) }
        } finally {
            lock.unlock()
        }
    }

    internal fun credit(amount: Amount) {
        check(lock.isHeldByCurrentThread) { "credit must run under the wallet lock" }
        balances[amount.currency] = (balances[amount.currency] ?: 0) + amount.units
    }

    internal fun debit(amount: Amount) {
        check(lock.isHeldByCurrentThread) { "debit must run under the wallet lock" }
        val held = balances[amount.currency] ?: 0
        if (held < amount.units) {
            throw InsufficientBalanceException(id, amount, Amount(held, amount.currency))
        }
        balances[amount.currency] = held - amount.units
    }

    override fun toString(): String {
        return "Wallet $id ($userId) ${allBalances().values}"
    }
}
