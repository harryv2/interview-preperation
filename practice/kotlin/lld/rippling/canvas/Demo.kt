package lld.rippling.canvas

fun main() {
    val canvas = Canvas(10, 5)

    println("-- draw two rectangles, later one is on top")
    val a = canvas.draw(0, 0, 4, 3, 'A')
    val b = canvas.draw(2, 1, 4, 3, 'B')
    println(canvas.render())
    println("  top at (3,2) = ${canvas.topAt(3, 2)?.fill}")

    println("-- move A, it comes to the front")
    canvas.move(a.id, 1, 1)
    println(canvas.render())

    println("-- move out of bounds is rejected")
    runCatching { canvas.move(b.id, 10, 0) }.onFailure { println("  ${it.message}") }

    println("-- clamp policy pins it to the edge instead")
    val clamped = Canvas(10, 5, BoundsPolicy.CLAMP)
    val c = clamped.draw(0, 0, 3, 2, 'C')
    clamped.move(c.id, 100, 100)
    println(clamped.render())
}
