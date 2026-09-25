package lld.fintech.cryptowallet.entity


enum class Currency(val code: String, val decimals: Int) {
    BTC("BTC", 8),
    USDT("USDT", 6),
    INR("INR", 2)
}


data class Amount(
    val units: Long,
    val currency: Currency
) : Comparable<Amount> {

    init {
        require(units >= 0) { "Amount can not be negative" }
    }

    operator fun plus(other: Amount): Amount {
        requireSameCurrency(other)
        return Amount(units + other.units, currency)
    }

    operator fun minus(other: Amount): Amount {
        requireSameCurrency(other)
        return Amount(units - other.units, currency)
    }

    override fun compareTo(other: Amount): Int {
        requireSameCurrency(other)
        return units.compareTo(other.units)
    }

    private fun requireSameCurrency(other: Amount) {
        require(currency == other.currency) { "Can not mix ${currency.code} and ${other.currency.code}" }
    }

    override fun toString(): String {
        val scale = generateSequence(1L) { it * 10 }.take(currency.decimals + 1).last()
        val whole = units / scale
        val fraction = units % scale
        return "$whole.${fraction.toString().padStart(currency.decimals, '0')} ${currency.code}"
    }

    companion object {
        fun zero(currency: Currency): Amount {
            return Amount(0, currency)
        }

        fun of(whole: Long, currency: Currency): Amount {
            val scale = generateSequence(1L) { it * 10 }.take(currency.decimals + 1).last()
            return Amount(whole * scale, currency)
        }
    }
}
