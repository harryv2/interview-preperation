package lld.rippling.aliendictionary

fun main() {
    println("-- classic")
    println("  ${AlienDictionary.order(listOf("wrt", "wrf", "er", "ett", "rftt"))}")

    println("-- prefix after a longer word is invalid")
    println("  '${AlienDictionary.order(listOf("abc", "ab"))}'")

    println("-- cycle is invalid")
    println("  '${AlienDictionary.order(listOf("z", "x", "z"))}'")

    println("-- letters with no constraint still appear")
    println("  ${AlienDictionary.order(listOf("ab", "adc"))}")
}
