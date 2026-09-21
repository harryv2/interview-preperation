package dsa.williamfiset.suffixarray

class SuffixArray(
    private val text: String
) {

    private val n = text.length

    val sa = IntArray(n)
    val lcp = IntArray(n)

    init {
        buildSuffixArray()
        buildLcpArray()
    }

    private class SuffixRank(
        var firstHalf: Int = 0,
        var secondHalf: Int = 0,
        var originalIndex: Int = 0
    )

    private fun buildSuffixArray() {
        var ranks = IntArray(n) { text[it].code }
        val tuples = Array(n) { SuffixRank() }

        var pos = 1
        while (pos < n) {
            for (i in 0 until n) {
                tuples[i].firstHalf = ranks[i]
                tuples[i].secondHalf = if (i + pos < n) ranks[i + pos] else -1
                tuples[i].originalIndex = i
            }

            tuples.sortWith(compareBy({ it.firstHalf }, { it.secondHalf }))

            val newRanks = IntArray(n)
            var rank = 0
            newRanks[tuples[0].originalIndex] = 0
            for (i in 1 until n) {
                val prev = tuples[i - 1]
                val curr = tuples[i]
                if (curr.firstHalf != prev.firstHalf || curr.secondHalf != prev.secondHalf) {
                    rank++
                }
                newRanks[curr.originalIndex] = rank
            }
            ranks = newRanks

            if (rank == n - 1) {
                break
            }
            pos *= 2
        }

        for (i in 0 until n) {
            sa[i] = tuples[i].originalIndex
        }
    }

    private fun buildLcpArray() {
        val inv = IntArray(n)
        for (i in 0 until n) {
            inv[sa[i]] = i
        }

        var len = 0
        for (i in 0 until n) {
            if (inv[i] == 0) {
                len = 0
                continue
            }
            val k = sa[inv[i] - 1]
            while (i + len < n && k + len < n && text[i + len] == text[k + len]) {
                len++
            }
            lcp[inv[i]] = len
            if (len > 0) {
                len--
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("-----i-----SA-----LCP---Suffix\n")
        for (i in 0 until n) {
            sb.append(String.format("%7d %7d %7d %s\n", i, sa[i], lcp[i], text.substring(sa[i])))
        }
        return sb.toString()
    }
}


class SuffixArraySlow(
    private val text: String
) {

    private val n = text.length

    val sa = (0 until n).sortedBy { text.substring(it) }.toIntArray()
    val lcp = IntArray(n)

    init {
        for (i in 1 until n) {
            lcp[i] = commonPrefix(sa[i - 1], sa[i])
        }
    }

    private fun commonPrefix(a: Int, b: Int): Int {
        var len = 0
        while (a + len < n && b + len < n && text[a + len] == text[b + len]) {
            len++
        }
        return len
    }
}


// longest substring shared by at least k of the strings, one answer
fun longestCommonSubstring(strings: List<String>, k: Int = strings.size): String {
    val numStrings = strings.size
    require(numStrings >= 2) { "need at least two strings" }
    require(k in 2..numStrings) { "k must be in 2..$numStrings" }

    val sb = StringBuilder()
    val owner = IntArray(strings.sumOf { it.length } + numStrings)
    for ((index, s) in strings.withIndex()) {
        for (c in s) {
            owner[sb.length] = index
            sb.append(c)
        }
        owner[sb.length] = -1
        sb.append(Char(index + 1))
    }

    val text = sb.toString()
    val n = text.length
    val suffixArray = SuffixArray(text)
    val sa = suffixArray.sa
    val lcp = suffixArray.lcp

    val count = IntArray(numStrings)
    var distinct = 0
    val window = ArrayDeque<Int>()
    var best = 0
    var bestStart = 0

    var lo = 0
    for (hi in 0 until n) {
        val ownerHi = owner[sa[hi]]
        if (ownerHi != -1) {
            if (count[ownerHi] == 0) {
                distinct++
            }
            count[ownerHi]++
        }
        if (hi > lo) {
            while (window.isNotEmpty() && lcp[window.last()] >= lcp[hi]) {
                window.removeLast()
            }
            window.addLast(hi)
        }

        while (distinct >= k) {
            if (window.isNotEmpty() && lcp[window.first()] > best) {
                best = lcp[window.first()]
                bestStart = sa[hi]
            }

            val ownerLo = owner[sa[lo]]
            if (ownerLo != -1) {
                count[ownerLo]--
                if (count[ownerLo] == 0) {
                    distinct--
                }
            }
            lo++
            while (window.isNotEmpty() && window.first() <= lo) {
                window.removeFirst()
            }
        }
    }

    return text.substring(bestStart, bestStart + best)
}


// WilliamFiset: longest substring(s) shared by at least k of the strings, all answers of maximum length
fun longestCommonSubstrings(strings: List<String>, k: Int = strings.size): Set<String> {
    val numStrings = strings.size
    require(numStrings >= 2) { "need at least two strings" }
    require(k in 2..numStrings) { "k must be in 2..$numStrings" }

    // concatenate with unique sentinels, remembering which string (colour) owns each position
    val sb = StringBuilder()
    val color = IntArray(strings.sumOf { it.length } + numStrings)
    for ((index, s) in strings.withIndex()) {
        for (c in s) {
            color[sb.length] = index
            sb.append(c)
        }
        color[sb.length] = -1
        sb.append(Char(index + 1))
    }

    val text = sb.toString()
    val n = text.length
    val suffixArray = SuffixArray(text)
    val sa = suffixArray.sa
    val lcp = suffixArray.lcp

    val colorCount = IntArray(numStrings)
    var colorsInWindow = 0
    val window = ArrayDeque<Int>()
    var best = 0
    val result = sortedSetOf<String>()

    // slide a window [lo, hi] over the sorted suffixes; once it holds k colours its
    // common prefix is min(lcp[lo + 1..hi]), tracked with a monotonic deque
    var lo = 0
    for (hi in 0 until n) {
        val colorHi = color[sa[hi]]
        if (colorHi != -1) {
            if (colorCount[colorHi] == 0) {
                colorsInWindow++
            }
            colorCount[colorHi]++
        }
        if (hi > lo) {
            while (window.isNotEmpty() && lcp[window.last()] >= lcp[hi]) {
                window.removeLast()
            }
            window.addLast(hi)
        }

        while (colorsInWindow >= k) {
            if (window.isNotEmpty()) {
                val windowLcp = lcp[window.first()]
                if (windowLcp > best) {
                    best = windowLcp
                    result.clear()
                }
                if (windowLcp == best && best > 0) {
                    result.add(text.substring(sa[hi], sa[hi] + windowLcp))
                }
            }

            val colorLo = color[sa[lo]]
            if (colorLo != -1) {
                colorCount[colorLo]--
                if (colorCount[colorLo] == 0) {
                    colorsInWindow--
                }
            }
            lo++
            while (window.isNotEmpty() && window.first() <= lo) {
                window.removeFirst()
            }
        }
    }

    return result
}


fun main() {

    val suffixArray = SuffixArray("ABBABAABAA")

    println(suffixArray.sa.toList())    // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]
    println(suffixArray.lcp.toList())   // [0, 1, 2, 1, 4, 2, 0, 3, 2, 1]
    println(suffixArray)


    val slow = SuffixArraySlow("ABBABAABAA")

    println(slow.sa.toList())           // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]
    println(slow.lcp.toList())          // [0, 1, 2, 1, 4, 2, 0, 3, 2, 1]


    println(longestCommonSubstring(listOf("ABABC", "BABCA", "ABCBA")))            // ABC
    println(longestCommonSubstring(listOf("XYZABCD", "ABCDXYZ", "PQABCDR")))      // ABCD
    println(longestCommonSubstring(listOf("ABC", "DEF", "GHI")).isEmpty())        // true


    val strings = listOf("AABC", "BCDC", "BCDE", "CDED")

    println(longestCommonSubstring(strings, 2))                            // BCD or CDE
    println(longestCommonSubstring(strings, 3))                            // BC or CD

    println(longestCommonSubstrings(strings, 2))                           // [BCD, CDE]
    println(longestCommonSubstrings(strings, 3))                           // [BC, CD]
    println(longestCommonSubstrings(strings))                              // [C]
    println(longestCommonSubstrings(listOf("ABC", "DEF", "GHI")))          // []

}
