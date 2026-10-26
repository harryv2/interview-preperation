package lld.rippling.canvas.entity

data class Rectangle(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val fill: Char,
) {
    fun contains(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }

    fun movedBy(dx: Int, dy: Int): Rectangle {
        return copy(x = x + dx, y = y + dy)
    }
}
