package lld.leaderboard

import lld.leaderboard.entity.Period
import lld.leaderboard.entity.ScoreEntry
import lld.leaderboard.service.InMemoryScoreRepository
import lld.leaderboard.service.LeaderboardService
import lld.leaderboard.strategy.BestScoreWins
import lld.leaderboard.structure.OrderStatisticTree
import lld.leaderboard.structure.RankedSet
import lld.leaderboard.structure.TreeSetRankedSet
import java.time.Instant
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun main() {

    val repository = InMemoryScoreRepository()
    val service = LeaderboardService(repository, BestScoreWins())
    val monday = Instant.parse("2026-09-21T10:00:00Z")

    banner("submit some runs")
    val runs = listOf(
        "aarav" to 4200L,
        "bhavna" to 9100L,
        "chirag" to 7350L,
        "divya" to 9100L,
        "esha" to 1500L,
        "farhan" to 6800L,
        "gita" to 8800L
    )
    runs.forEachIndexed { index, (player, score) ->
        service.submit("runner", player, score, monday.plusSeconds(index * 60L))
    }
    service.top("runner", Period.ALL_TIME, 10, monday).forEach { println("  $it") }

    banner("bhavna and divya tied on 9100, the earlier run holds the higher rank")
    println("  bhavna #${service.rankOf("runner", "bhavna", Period.ALL_TIME, monday)}")
    println("  divya  #${service.rankOf("runner", "divya", Period.ALL_TIME, monday)}")

    banner("best score wins, a worse run does not move anyone")
    service.submit("runner", "gita", 2000, monday.plusSeconds(4000))
    println("  gita still on ${service.scoreOf("runner", "gita", Period.ALL_TIME, monday)} at #${service.rankOf("runner", "gita", Period.ALL_TIME, monday)}")
    service.submit("runner", "esha", 9500, monday.plusSeconds(5000))
    println("  esha beat her own run, now #${service.rankOf("runner", "esha", Period.ALL_TIME, monday)}")

    banner("the slice around one player, which is the screen nobody can serve without O(log n) select")
    service.around("runner", "farhan", Period.ALL_TIME, 2, monday).forEach { println("  $it") }

    banner("daily and all time are different boards")
    val tuesday = monday.plusSeconds(60 * 60 * 24)
    service.submit("runner", "zara", 500, tuesday)
    println("  all time: ${service.top("runner", Period.ALL_TIME, 3, tuesday)}")
    println("  tuesday:  ${service.top("runner", Period.DAILY, 3, tuesday)}")

    banner("cold start, a fresh service rebuilds the board from the durable copy")
    val restarted = LeaderboardService(repository, BestScoreWins())
    println("  esha is still #${restarted.rankOf("runner", "esha", Period.ALL_TIME, monday)} after a restart")

    banner("why not TreeSet")
    benchmark()
}

private fun benchmark() {
    val players = 100_000
    val lookups = 500
    val random = Random(7)

    val entries = (0 until players).map {
        ScoreEntry("p$it", random.nextLong(1_000_000), Instant.EPOCH.plusSeconds(it.toLong()))
    }
    val probes = (0 until lookups).map { entries[random.nextInt(players)] }

    listOf<Pair<String, RankedSet<ScoreEntry>>>(
        "OrderStatisticTree" to OrderStatisticTree(ScoreEntry.ORDER),
        "TreeSetRankedSet" to TreeSetRankedSet(ScoreEntry.ORDER)
    ).forEach { (name, set) ->
        val build = measureTimeMillis { entries.forEach { set.insert(it) } }
        var sink = 0
        val rank = measureTimeMillis { probes.forEach { sink += set.rankOf(it) } }
        val select = measureTimeMillis { repeat(lookups) { set.select(random.nextInt(players)) } }

        println("  ${name.padEnd(20)} build $players: ${build}ms, $lookups rankOf: ${rank}ms, $lookups select: ${select}ms")
        check(sink >= 0)
    }
    println("  both order in O(log n), only one of them can say where you stand in O(log n)")
}

private fun banner(title: String) {
    println("\n== $title ==")
}
