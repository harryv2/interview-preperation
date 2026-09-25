package lld.fintech.cryptowallet.service

import lld.fintech.cryptowallet.entity.Amount
import lld.fintech.cryptowallet.entity.Currency
import lld.fintech.cryptowallet.entity.EntryType
import lld.fintech.cryptowallet.entity.LedgerEntry
import lld.fintech.cryptowallet.entity.Transaction
import lld.fintech.cryptowallet.entity.Wallet
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.withLock


class WalletService {

    private val wallets = ConcurrentHashMap<String, Wallet>()
    private val ledger = ConcurrentHashMap<String, MutableList<LedgerEntry>>()
    private val transactions = ConcurrentHashMap<String, Transaction>()
    private val idempotency = ConcurrentHashMap<String, String>()

    fun openWallet(userId: String): Wallet {
        val wallet = Wallet("W-${UUID.randomUUID().toString().take(6)}", userId)
        wallets[wallet.id] = wallet
        ledger[wallet.id] = mutableListOf()
        return wallet
    }

    fun deposit(walletId: String, amount: Amount): Transaction {
        require(amount.units > 0) { "Deposit must be positive" }
        val wallet = wallet(walletId)

        wallet.lock.withLock {
            wallet.credit(amount)
            val entry = record(wallet, EntryType.DEPOSIT, amount)
            return commit(Transaction(entry.transactionId, null, walletId, amount, listOf(entry), entry.at))
        }
    }

    fun withdraw(walletId: String, amount: Amount): Transaction {
        require(amount.units > 0) { "Withdrawal must be positive" }
        val wallet = wallet(walletId)

        wallet.lock.withLock {
            wallet.debit(amount)
            val entry = record(wallet, EntryType.WITHDRAWAL, amount)
            return commit(Transaction(entry.transactionId, walletId, null, amount, listOf(entry), entry.at))
        }
    }

    fun transfer(fromWalletId: String, toWalletId: String, amount: Amount, idempotencyKey: String? = null): Transaction {
        require(amount.units > 0) { "Transfer must be positive" }
        require(fromWalletId != toWalletId) { "Can not transfer to the same wallet" }

        idempotencyKey?.let { key ->
            idempotency[key]?.let { return transactions.getValue(it) }
        }

        val from = wallet(fromWalletId)
        val to = wallet(toWalletId)

        // always lock in id order, otherwise A->B and B->A deadlock
        val ordered = listOf(from, to).sortedBy { it.id }

        ordered[0].lock.withLock {
            ordered[1].lock.withLock {
                idempotencyKey?.let { key ->
                    idempotency[key]?.let { return transactions.getValue(it) }
                }

                from.debit(amount)
                to.credit(amount)

                val transactionId = "T-${UUID.randomUUID().toString().take(8)}"
                val out = record(from, EntryType.TRANSFER_OUT, amount, transactionId)
                val into = record(to, EntryType.TRANSFER_IN, amount, transactionId)

                val transaction = commit(Transaction(transactionId, fromWalletId, toWalletId, amount, listOf(out, into), out.at))
                idempotencyKey?.let { idempotency[it] = transactionId }
                return transaction
            }
        }
    }

    fun historyOf(walletId: String): List<LedgerEntry> {
        wallet(walletId)
        return ledger.getValue(walletId).toList()
    }

    fun totalHeld(currency: Currency): Amount {
        var total = 0L
        wallets.values.forEach {
            total += it.balanceOf(currency).units
        }
        return Amount(total, currency)
    }

    private fun record(
        wallet: Wallet,
        type: EntryType,
        amount: Amount,
        transactionId: String = "T-${UUID.randomUUID().toString().take(8)}"
    ): LedgerEntry {
        val entry = LedgerEntry(
            transactionId = transactionId,
            walletId = wallet.id,
            type = type,
            amount = amount,
            balanceAfter = wallet.balanceOf(amount.currency),
            at = Instant.now()
        )
        ledger.getValue(wallet.id).add(entry)
        return entry
    }

    private fun commit(transaction: Transaction): Transaction {
        transactions[transaction.id] = transaction
        return transaction
    }

    private fun wallet(walletId: String): Wallet {
        val wallet = wallets[walletId]
        requireNotNull(wallet) { "No wallet $walletId" }
        return wallet
    }
}
