package lld.rippling.kvstore

fun main() {
    val store = KeyValueStore()

    println("-- plain get / set / delete")
    store.set("a", "1")
    store.set("b", "2")
    println("  a=${store.get("a")} b=${store.get("b")}")
    store.delete("b")
    println("  b after delete=${store.get("b")}")

    println("-- rollback drops writes")
    store.begin()
    store.set("a", "10")
    store.delete("a")
    println("  inside tx a=${store.get("a")}")
    store.rollback()
    println("  after rollback a=${store.get("a")}")

    println("-- nested commit only lands when the outer one commits")
    store.begin()
    store.set("x", "outer")
    store.begin()
    store.set("x", "inner")
    store.set("y", "inner")
    store.commit()
    println("  after inner commit x=${store.get("x")} y=${store.get("y")}")
    store.rollback()
    println("  after outer rollback x=${store.get("x")} y=${store.get("y")}")

    println("-- commit with no transaction")
    runCatching { store.commit() }.onFailure { println("  ${it.message}") }
}
