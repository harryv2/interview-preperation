package dsa.williamfiset.suffixarray

class SuffixArraySaIs(
    private val text: String
) {

    val sa: IntArray

    init {
        val s = IntArray(text.length) { text[it].code }
        val upper = s.maxOrNull() ?: 0
        sa = saIs(s, upper)
    }

    // s holds values in 0..upper, an implicit sentinel smaller than everything ends it
    private fun saIs(s: IntArray, upper: Int): IntArray {
        val n = s.size
        if (n == 0) {
            return IntArray(0)
        }
        if (n == 1) {
            return intArrayOf(0)
        }
        if (n == 2) {
            return if (s[0] < s[1]) intArrayOf(0, 1) else intArrayOf(1, 0)
        }

        val sa = IntArray(n)

        // isS[i] = suffix i is S-type (smaller than suffix i + 1); last suffix is L-type
        val isS = BooleanArray(n)
        for (i in n - 2 downTo 0) {
            isS[i] = if (s[i] == s[i + 1]) isS[i + 1] else s[i] < s[i + 1]
        }

        // sumL[c] = start of L bucket for c, sumS[c] = start of S bucket for c
        val sumL = IntArray(upper + 1)
        val sumS = IntArray(upper + 1)
        for (i in 0 until n) {
            if (!isS[i]) {
                sumS[s[i]]++
            } else {
                sumL[s[i] + 1]++
            }
        }
        for (i in 0..upper) {
            sumS[i] += sumL[i]
            if (i < upper) {
                sumL[i + 1] += sumS[i]
            }
        }

        // place LMS suffixes, induce L-types left to right, then S-types right to left
        fun induce(lms: IntArray) {
            sa.fill(-1)
            val buf = IntArray(upper + 1)
            sumS.copyInto(buf)
            for (d in lms) {
                if (d == n) {
                    continue
                }
                sa[buf[s[d]]++] = d
            }
            sumL.copyInto(buf)
            sa[buf[s[n - 1]]++] = n - 1
            for (i in 0 until n) {
                val v = sa[i]
                if (v >= 1 && !isS[v - 1]) {
                    sa[buf[s[v - 1]]++] = v - 1
                }
            }
            sumL.copyInto(buf)
            for (i in n - 1 downTo 0) {
                val v = sa[i]
                if (v >= 1 && isS[v - 1]) {
                    sa[--buf[s[v - 1] + 1]] = v - 1
                }
            }
        }

        // LMS = leftmost S-type: an S-type suffix whose left neighbour is L-type
        val lmsMap = IntArray(n + 1) { -1 }
        var m = 0
        for (i in 1 until n) {
            if (!isS[i - 1] && isS[i]) {
                lmsMap[i] = m++
            }
        }
        val lms = IntArray(m)
        var idx = 0
        for (i in 1 until n) {
            if (!isS[i - 1] && isS[i]) {
                lms[idx++] = i
            }
        }

        induce(lms)

        if (m == 0) {
            return sa
        }

        val sortedLms = IntArray(m)
        idx = 0
        for (v in sa) {
            if (lmsMap[v] != -1) {
                sortedLms[idx++] = v
            }
        }

        // name each LMS substring; equal substrings get equal names
        val reduced = IntArray(m)
        var reducedUpper = 0
        reduced[lmsMap[sortedLms[0]]] = 0
        for (i in 1 until m) {
            var l = sortedLms[i - 1]
            var r = sortedLms[i]
            val endL = if (lmsMap[l] + 1 < m) lms[lmsMap[l] + 1] else n
            val endR = if (lmsMap[r] + 1 < m) lms[lmsMap[r] + 1] else n
            var same = true
            if (endL - l != endR - r) {
                same = false
            } else {
                while (l < endL) {
                    if (s[l] != s[r]) {
                        break
                    }
                    l++
                    r++
                }
                if (l == n || r == n || s[l] != s[r]) {
                    same = false
                }
            }
            if (!same) {
                reducedUpper++
            }
            reduced[lmsMap[sortedLms[i]]] = reducedUpper
        }

        // recurse on the reduced problem (at most n / 2 long), then induce the final order
        val reducedSa = saIs(reduced, reducedUpper)
        for (i in 0 until m) {
            sortedLms[i] = lms[reducedSa[i]]
        }
        induce(sortedLms)

        return sa
    }
}


fun main() {

    val text = "ABBABAABAA"

    println(SuffixArraySaIs(text).sa.toList())   // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]

}
