package dsa.williamfiset.suffixarray

class SuffixArrayFast(
    private val text: String
) {

    private val n = text.length

    val sa = IntArray(n)

    init {
        buildSuffixArray()
    }

    private fun buildSuffixArray() {
        if (n == 0) {
            return
        }
        var rank = IntArray(n) { text[it].code }
        var sa2 = IntArray(n)
        var alphabetSize = maxOf(256, (rank.maxOrNull() ?: 0) + 1)
        val c = IntArray(maxOf(alphabetSize, n))

        for (i in 0 until n) {
            c[rank[i]]++
        }
        for (i in 1 until alphabetSize) {
            c[i] += c[i - 1]
        }
        for (i in n - 1 downTo 0) {
            sa[--c[rank[i]]] = i
        }

        var p = 1
        while (p < n) {
            // sa2 = suffixes ordered by their second half (rank[i + p]); suffixes with no second half come first
            var r = 0
            for (i in n - p until n) {
                sa2[r++] = i
            }
            for (i in 0 until n) {
                if (sa[i] >= p) {
                    sa2[r++] = sa[i] - p
                }
            }

            // stable counting sort of sa2 by first half (rank[i]) gives the order by (first, second)
            c.fill(0, 0, alphabetSize)
            for (i in 0 until n) {
                c[rank[i]]++
            }
            for (i in 1 until alphabetSize) {
                c[i] += c[i - 1]
            }
            for (i in n - 1 downTo 0) {
                sa[--c[rank[sa2[i]]]] = sa2[i]
            }

            r = 0
            sa2[sa[0]] = 0
            for (i in 1 until n) {
                val prev = sa[i - 1]
                val curr = sa[i]
                val secondPrev = if (prev + p < n) rank[prev + p] else -1
                val secondCurr = if (curr + p < n) rank[curr + p] else -1
                if (rank[prev] != rank[curr] || secondPrev != secondCurr) {
                    r++
                }
                sa2[curr] = r
            }

            val tmp = rank
            rank = sa2
            sa2 = tmp

            if (r == n - 1) {
                break
            }
            alphabetSize = r + 1
            p = p shl 1
        }
    }
}


fun main() {

    val text = "ABBABAABAA"

    println(SuffixArrayFast(text).sa.toList())   // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]

}
