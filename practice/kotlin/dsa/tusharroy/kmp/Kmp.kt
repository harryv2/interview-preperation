package dsa.tusharroy.kmp

fun hasSubstring(text: String, pattern: String): Boolean {
    var i = 0
    var j = 0
    var k = 0
    while (i < text.length && j < pattern.length) {
        if (text[i] == pattern[j]) {
            i++
            j++
        } else {
            j = 0
            k++
            i = k
        }
    }
    return j == pattern.length
}

// lps[i] = length of the longest proper prefix of pattern[0..i] that is also its suffix
fun computeTemporaryArray(pattern: String): IntArray {
    val lps = IntArray(pattern.length)
    var index = 0
    var i = 1
    while (i < pattern.length) {
        if (pattern[i] == pattern[index]) {
            lps[i] = index + 1
            index++
            i++
        } else {
            if (index != 0) {
                index = lps[index - 1]
            } else {
                lps[i] = 0
                i++
            }
        }
    }
    return lps
}

fun kmp(text: String, pattern: String): Boolean {
    val lps = computeTemporaryArray(pattern)
    var i = 0
    var j = 0
    while (i < text.length && j < pattern.length) {
        if (text[i] == pattern[j]) {
            i++
            j++
        } else {
            if (j != 0) {
                j = lps[j - 1]
            } else {
                i++
            }
        }
    }
    return j == pattern.length
}


fun main() {

    println(computeTemporaryArray("aabaabaaa").toList())    // [0, 1, 0, 1, 2, 3, 4, 5, 2]
    println(computeTemporaryArray("abcdabcy").toList())     // [0, 0, 0, 0, 1, 2, 3, 0]

    println(kmp("abcxabcdabcdabcy", "abcdabcy"))           // true
    println(hasSubstring("abcxabcdabcdabcy", "abcdabcy"))  // true
    println(kmp("abcxabcdabcdabcy", "abcdabcz"))           // false

}
