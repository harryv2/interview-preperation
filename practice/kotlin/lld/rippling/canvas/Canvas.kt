package lld.rippling.canvas

import lld.rippling.canvas.entity.Rectangle

enum class BoundsPolicy { REJECT, CLAMP }

class Canvas(
    private val width: Int,
    private val height: Int,
    private val policy: BoundsPolicy = BoundsPolicy.REJECT,
) {
    private val shapes = LinkedHashMap<Int, Rectangle>()
    private var nextId = 1

    fun draw(x: Int, y: Int, w: Int, h: Int, fill: Char): Rectangle {
        require(w > 0 && h > 0) { "Size must be positive" }
        require(w <= width && h <= height) { "Rectangle larger than canvas" }
        val rect = fit(Rectangle(nextId++, x, y, w, h, fill))
        shapes[rect.id] = rect
        return rect
    }

    fun move(id: Int, dx: Int, dy: Int): Rectangle {
        val rect = requireNotNull(shapes[id]) { "No rectangle $id" }
        val moved = fit(rect.movedBy(dx, dy))
        shapes.remove(id)
        shapes[id] = moved
        return moved
    }

    fun erase(id: Int) {
        shapes.remove(id)
    }

    fun topAt(x: Int, y: Int): Rectangle? {
        return shapes.values.lastOrNull { it.contains(x, y) }
    }

    fun render(): String {
        val grid = Array(height) { CharArray(width) { '.' } }
        for (rect in shapes.values) {
            for (row in rect.y until rect.y + rect.height) {
                for (col in rect.x until rect.x + rect.width) {
                    grid[row][col] = rect.fill
                }
            }
        }
        return grid.joinToString("\n") { String(it) }
    }

    private fun fit(rect: Rectangle): Rectangle {
        val inside = rect.x >= 0 && rect.y >= 0 && rect.x + rect.width <= width && rect.y + rect.height <= height
        if (inside) {
            return rect
        }
        if (policy == BoundsPolicy.REJECT) {
            throw IllegalArgumentException("Rectangle ${rect.id} out of bounds")
        }
        return rect.copy(
            x = rect.x.coerceIn(0, width - rect.width),
            y = rect.y.coerceIn(0, height - rect.height),
        )
    }
}
