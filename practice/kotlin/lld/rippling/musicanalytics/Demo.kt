package lld.rippling.musicanalytics

fun main() {
    val player = MusicPlayer()
    val one = player.addSong("Song One")
    val two = player.addSong("Song Two")
    val three = player.addSong("Song Three")
    val four = player.addSong("Song Four")

    player.play(one.id, "alice")
    player.play(one.id, "alice")
    player.play(one.id, "bob")
    player.play(two.id, "alice")
    player.play(three.id, "carol")
    player.play(three.id, "bob")
    player.play(three.id, "alice")
    player.play(four.id, "alice")

    println("-- analytics by unique listeners")
    player.printAnalytics()

    println("-- top 2")
    println("  ${player.topSongs(2).map { it.title }}")

    println("-- last 3 for alice (replaying moves a song to the front)")
    player.play(two.id, "alice")
    println("  ${player.recentlyPlayed("alice").map { it.title }}")

    println("-- generic queue: immutable items replaced by key")
    data class Task(val id: String, val priority: Int)
    val queue = IndexedPriorityQueue<String, Task>(compareBy { it.priority }) { it.id }
    queue.add(Task("a", 5))
    queue.add(Task("b", 3))
    queue.add(Task("c", 8))
    queue.update(Task("c", 1))
    queue.remove("b")
    println("  top 1 without removing: ${queue.top(1)}")
    val drained = ArrayList<Task>()
    while (!queue.isEmpty()) {
        drained += queue.poll()!!
    }
    println("  $drained")
}
