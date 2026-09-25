package lld.misc.trello

import lld.misc.trello.entity.CardList
import lld.misc.trello.entity.Filters
import lld.misc.trello.service.TrelloService
import java.time.Instant

fun main() {

    val service = TrelloService()
    val ravi = service.register("Ravi")
    val meena = service.register("Meena")

    val board = service.createBoard("Sprint 42")
    val backlog = board.addList("Backlog")
    val doing = board.addList("In Progress")
    val done = board.addList("Done")

    val auth = board.addCard(backlog.id, "Fix auth redirect")
    val cache = board.addCard(backlog.id, "Cache invalidation on write")
    val docs = board.addCard(backlog.id, "Update onboarding docs")

    println(board)
    println("  Backlog: ${backlog.cards()} at ${backlog.positions()}")

    println("\n== move across lists ==")
    board.moveCard(cache.id, doing.id, 0)
    board.moveCard(auth.id, doing.id, 0)
    println("  Backlog: ${backlog.cards()}")
    println("  In Progress: ${doing.cards()} at ${doing.positions()}")

    println("\n== reorder inside a list, only the moved card is written ==")
    board.moveCard(auth.id, doing.id, 1)
    println("  In Progress: ${doing.cards()} at ${doing.positions()}")

    println("\n== assign ==")
    board.assign(auth.id, ravi.id)
    board.assign(cache.id, ravi.id)
    board.assign(docs.id, meena.id)
    auth.labels.add("bug")
    docs.labels.add("chore")
    auth.dueAt = Instant.parse("2026-09-24T17:00:00Z")

    println("\n== filter and search ==")
    val now = Instant.parse("2026-09-25T09:00:00Z")
    println("  assigned to Ravi:  ${board.search(Filters.assignedTo(ravi.id))}")
    println("  in Backlog:        ${board.search(Filters.inList(backlog.id))}")
    println("  labelled bug:      ${board.search(Filters.labelled("bug"))}")
    println("  text 'cache':      ${board.search(Filters.textContains("cache"))}")
    println("  overdue:           ${board.overdue(now)}")

    println("\n== composed ==")
    println("  Ravi's bugs:       ${board.search(Filters.assignedTo(ravi.id) and Filters.labelled("bug"))}")
    println("  bug or chore:      ${board.search(Filters.labelled("bug") or Filters.labelled("chore"))}")
    println("  not in Done:       ${board.search(!Filters.inList(done.id))}")
    println("  unassigned:        ${board.search(Filters.unassigned())}")

    println("\n== the gap runs out ==")
    val squeeze = board.addList("Squeeze")
    board.addCard(squeeze.id, "top")
    board.addCard(squeeze.id, "bottom")
    println("  two appended:      ${squeeze.positions()}")

    repeat(6) { board.addCard(squeeze.id, "wedge $it", index = 1) }
    println("  six into one gap:  ${squeeze.positions()}")
    println("  gap of ${CardList.GAP} allows about six, the seventh forces a respace")

    val orderBefore = squeeze.cards().map { it.title }
    board.addCard(squeeze.id, "one more", index = 1)
    val orderAfter = squeeze.cards().map { it.title }
    println("  after respace:     ${squeeze.positions()}")
    println("  order kept:        ${orderAfter.filter { it in orderBefore } == orderBefore}")
    println("  $orderAfter")
}
