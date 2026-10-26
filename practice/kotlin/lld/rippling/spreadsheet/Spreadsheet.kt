package lld.rippling.spreadsheet

sealed interface CellValue {
    data class Num(val value: Long) : CellValue
    data class Text(val value: String) : CellValue
    data class Error(val reason: String) : CellValue
    data object Empty : CellValue
}

class Spreadsheet {
    private val raw = HashMap<String, String>()
    private val formulas = HashMap<String, Formula>()
    private val dependencies = HashMap<String, Set<String>>()
    private val dependents = HashMap<String, HashSet<String>>()
    private val cache = HashMap<String, CellValue>()

    fun set(cell: String, input: String) {
        val formula = if (input.startsWith("=")) Formula.parse(input) else null
        val refs = formula?.references ?: emptySet()
        require(!createsCycle(cell, refs)) { "Setting $cell to $input creates a cycle" }

        for (old in dependencies[cell].orEmpty()) {
            dependents[old]?.remove(cell)
        }
        for (ref in refs) {
            dependents.getOrPut(ref) { HashSet() }.add(cell)
        }
        dependencies[cell] = refs

        raw[cell] = input
        if (formula != null) {
            formulas[cell] = formula
        } else {
            formulas.remove(cell)
        }
        invalidate(cell)
    }

    fun get(cell: String): CellValue {
        cache[cell]?.let { return it }
        val value = evaluate(cell)
        cache[cell] = value
        return value
    }

    fun raw(cell: String): String? {
        return raw[cell]
    }

    private fun evaluate(cell: String): CellValue {
        val input = raw[cell] ?: return CellValue.Empty
        val formula = formulas[cell]
        if (formula == null) {
            val number = input.toLongOrNull()
            return if (number != null) CellValue.Num(number) else CellValue.Text(input)
        }

        var total = 0L
        for (term in formula.terms) {
            if (term.ref == null) {
                total += term.sign * term.literal
                continue
            }
            when (val value = get(term.ref)) {
                is CellValue.Num -> total += term.sign * value.value
                is CellValue.Empty -> {}
                is CellValue.Text -> return CellValue.Error("#VALUE ${term.ref}")
                is CellValue.Error -> return value
            }
        }
        return CellValue.Num(total)
    }

    private fun createsCycle(cell: String, refs: Set<String>): Boolean {
        val stack = ArrayDeque(refs)
        val seen = HashSet<String>()
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current == cell) {
                return true
            }
            if (seen.add(current)) {
                stack.addAll(dependencies[current].orEmpty())
            }
        }
        return false
    }

    private fun invalidate(cell: String) {
        val queue = ArrayDeque(listOf(cell))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (cache.remove(current) != null || current == cell) {
                queue.addAll(dependents[current].orEmpty())
            }
        }
    }
}
