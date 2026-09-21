package dsa.williamfiset.suffixarray

class SuffixArrayDc3(
    private val text: String
) {

    val sa: IntArray

    init {
        val n = text.length
        if (n == 0) {
            sa = IntArray(0)
        } else if (n == 1) {
            sa = intArrayOf(0)
        } else {
            val s = IntArray(n + 3)
            var k = 0
            for (i in 0 until n) {
                s[i] = text[i].code + 1
                k = maxOf(k, s[i])
            }
            sa = IntArray(n)
            skew(s, sa, n, k)
        }
    }

    private fun leq(a1: Int, a2: Int, b1: Int, b2: Int): Boolean {
        return a1 < b1 || (a1 == b1 && a2 <= b2)
    }

    private fun leq(a1: Int, a2: Int, a3: Int, b1: Int, b2: Int, b3: Int): Boolean {
        return a1 < b1 || (a1 == b1 && leq(a2, a3, b2, b3))
    }

    // stable counting sort of a[0 until n] into b, keyed by r[a[i] + offset] in 0..k
    private fun radixPass(a: IntArray, b: IntArray, r: IntArray, offset: Int, n: Int, k: Int) {
        val c = IntArray(k + 1)
        for (i in 0 until n) {
            c[r[a[i] + offset]]++
        }
        var sum = 0
        for (i in 0..k) {
            val t = c[i]
            c[i] = sum
            sum += t
        }
        for (i in 0 until n) {
            b[c[r[a[i] + offset]]++] = a[i]
        }
    }

    // s holds values in 1..k, s[n] = s[n + 1] = s[n + 2] = 0, n >= 2
    private fun skew(s: IntArray, sa: IntArray, n: Int, k: Int) {
        val n0 = (n + 2) / 3
        val n1 = (n + 1) / 3
        val n2 = n / 3
        val n02 = n0 + n2

        val s12 = IntArray(n02 + 3)
        val sa12 = IntArray(n02 + 3)
        val s0 = IntArray(n0)
        val sa0 = IntArray(n0)

        // positions of mod 1 and mod 2 suffixes; n0 - n1 adds a dummy mod 1 suffix when n % 3 == 1
        var j = 0
        for (i in 0 until n + (n0 - n1)) {
            if (i % 3 != 0) {
                s12[j++] = i
            }
        }

        // radix sort the mod 1 and mod 2 triples
        radixPass(s12, sa12, s, 2, n02, k)
        radixPass(sa12, s12, s, 1, n02, k)
        radixPass(s12, sa12, s, 0, n02, k)

        // lexicographic names of the triples
        var name = 0
        var c0 = -1
        var c1 = -1
        var c2 = -1
        for (i in 0 until n02) {
            val p = sa12[i]
            if (s[p] != c0 || s[p + 1] != c1 || s[p + 2] != c2) {
                name++
                c0 = s[p]
                c1 = s[p + 1]
                c2 = s[p + 2]
            }
            if (p % 3 == 1) {
                s12[p / 3] = name
            } else {
                s12[p / 3 + n0] = name
            }
        }

        // recurse if names are not unique yet
        if (name < n02) {
            skew(s12, sa12, n02, name)
            for (i in 0 until n02) {
                s12[sa12[i]] = i + 1
            }
        } else {
            for (i in 0 until n02) {
                sa12[s12[i] - 1] = i
            }
        }

        // sort mod 0 suffixes by first char, stable on the already sorted mod 1 ranks
        j = 0
        for (i in 0 until n02) {
            if (sa12[i] < n0) {
                s0[j++] = 3 * sa12[i]
            }
        }
        radixPass(s0, sa0, s, 0, n0, k)

        // merge the two sorted lists
        var p = 0
        var t = n0 - n1
        var out = 0
        while (out < n) {
            val i = if (sa12[t] < n0) sa12[t] * 3 + 1 else (sa12[t] - n0) * 3 + 2
            val jj = sa0[p]
            val from12 = if (sa12[t] < n0) {
                leq(s[i], s12[sa12[t] + n0], s[jj], s12[jj / 3])
            } else {
                leq(s[i], s[i + 1], s12[sa12[t] - n0 + 1], s[jj], s[jj + 1], s12[jj / 3 + n0])
            }
            if (from12) {
                sa[out] = i
                t++
                if (t == n02) {
                    out++
                    while (p < n0) {
                        sa[out++] = sa0[p++]
                    }
                }
            } else {
                sa[out] = jj
                p++
                if (p == n0) {
                    out++
                    while (t < n02) {
                        sa[out++] = if (sa12[t] < n0) sa12[t] * 3 + 1 else (sa12[t] - n0) * 3 + 2
                        t++
                    }
                }
            }
            out++
        }
    }
}


fun main() {

    val text = "ABBABAABAA"

    println(SuffixArrayDc3(text).sa.toList())   // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]

}
