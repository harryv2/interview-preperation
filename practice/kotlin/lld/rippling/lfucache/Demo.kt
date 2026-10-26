package lld.rippling.lfucache

fun main() {
    val cache = LfuCache<String, Int>(2)

    println("-- least frequent goes first")
    cache.put("a", 1)
    cache.put("b", 2)
    cache.get("a")
    cache.put("c", 3)
    println("  a=${cache.get("a")} b=${cache.get("b")} c=${cache.get("c")}")

    println("-- tie on frequency, least recent goes first")
    val tie = LfuCache<String, Int>(2)
    tie.put("x", 1)
    tie.put("y", 2)
    tie.put("z", 3)
    println("  x=${tie.get("x")} y=${tie.get("y")} z=${tie.get("z")}")

    println("-- update keeps the key and bumps its frequency")
    tie.put("y", 20)
    tie.put("w", 4)
    println("  y=${tie.get("y")} z=${tie.get("z")} w=${tie.get("w")}")
}
