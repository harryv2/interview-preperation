package dsa.leetcode_75

class SolutionSlidingWindow() {

    fun findAnagrams(s: String, p: String): List<Int> {
        var mapOfS = hashMapOf<Char, Int>()
        var mapOfP = hashMapOf<Char, Int>()

        if (p.length > s.length) {
            return emptyList()
        }


        for (c in p) {
            mapOfP[c] = mapOfP.getOrDefault(c, 0) + 1
        }

        for(i in 0..<p.length) {
            mapOfS[s[i]] = mapOfS.getOrDefault(s[i], 0) + 1
        }

        var low = 0
        var high = p.length
        val answers = mutableListOf<Int>()

        while (high < s.length ) {
            if(compareMap(mapOfS, mapOfP)) {
                answers.add(low)
            }
            mapOfS[s[low]] = mapOfS[s[low]]!! - 1
            mapOfS[s[high]] = mapOfS.getOrDefault(s[high], 0 ) + 1
            low++
            high++
        }

        if(compareMap(mapOfS, mapOfP)) {
            answers.add(low)
        }

        return answers

    }


    fun compareMap(bigger: Map<Char, Int>, smaller: Map<Char, Int>): Boolean {
        return smaller.all {
            bigger.getOrDefault(it.key, 0) == it.value
        }
    }
}

fun main() {
    val sol = SolutionSlidingWindow()


    println(sol.findAnagrams(s = "abab", p = "ab"))

}