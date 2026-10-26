package lld.rippling.spreadsheet

data class Term(
    val sign: Int,
    val ref: String?,
    val literal: Long,
)

class Formula(val terms: List<Term>) {

    val references: Set<String>
        get() = terms.mapNotNull { it.ref }.toSet()

    companion object {
        private val token = Regex("""[A-Z]+[0-9]+|\d+|[+-]""")

        fun parse(text: String): Formula {
            val body = text.removePrefix("=").replace(" ", "")
            val tokens = token.findAll(body).map { it.value }.toList()
            require(tokens.isNotEmpty() && tokens.joinToString("") == body) { "Bad formula $text" }

            val terms = ArrayList<Term>()
            var sign = 1
            var expectOperand = true
            for (t in tokens) {
                if (t == "+" || t == "-") {
                    require(expectOperand == terms.isEmpty()) { "Bad formula $text" }
                    sign = if (t == "-") -1 else 1
                    expectOperand = true
                    continue
                }
                require(expectOperand) { "Bad formula $text" }
                if (t[0].isDigit()) {
                    terms += Term(sign, null, t.toLong())
                } else {
                    terms += Term(sign, t, 0)
                }
                sign = 1
                expectOperand = false
            }
            require(!expectOperand) { "Bad formula $text" }
            return Formula(terms)
        }
    }
}
