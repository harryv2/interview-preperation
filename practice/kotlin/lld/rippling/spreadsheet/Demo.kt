package lld.rippling.spreadsheet

fun main() {
    val sheet = Spreadsheet()

    println("-- values and formulas")
    sheet.set("A1", "10")
    sheet.set("B1", "5")
    sheet.set("C1", "=A1+B1")
    sheet.set("D1", "=C1-3+A1")
    sheet.set("E1", "hello")
    println("  C1=${sheet.get("C1")} D1=${sheet.get("D1")} E1=${sheet.get("E1")}")

    println("-- changing A1 flows through the chain")
    sheet.set("A1", "100")
    println("  C1=${sheet.get("C1")} D1=${sheet.get("D1")}")

    println("-- empty cell is 0, text is an error")
    sheet.set("F1", "=Z9+1")
    sheet.set("G1", "=E1+1")
    println("  F1=${sheet.get("F1")} G1=${sheet.get("G1")}")

    println("-- cycles are rejected and the old value stays")
    runCatching { sheet.set("A1", "=D1") }.onFailure { println("  ${it.message}") }
    runCatching { sheet.set("H1", "=H1+1") }.onFailure { println("  ${it.message}") }
    println("  A1=${sheet.get("A1")}")
}
