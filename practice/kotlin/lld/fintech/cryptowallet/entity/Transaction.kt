package lld.fintech.cryptowallet.entity

import java.time.Instant


enum class EntryType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}


data class LedgerEntry(
    val transactionId: String,
    val walletId: String,
    val type: EntryType,
    val amount: Amount,
    val balanceAfter: Amount,
    val at: Instant
) {
    override fun toString(): String {
        return "$type $amount on $walletId -> $balanceAfter"
    }
}


data class Transaction(
    val id: String,
    val fromWalletId: String?,
    val toWalletId: String?,
    val amount: Amount,
    val entries: List<LedgerEntry>,
    val at: Instant
) {
    override fun toString(): String {
        val from = fromWalletId ?: "external"
        val to = toWalletId ?: "external"
        return "Tx $id  $amount  $from -> $to"
    }
}
