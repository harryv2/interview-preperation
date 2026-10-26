package lld.rippling.articlevotes

fun main() {
    val votes = ArticleVotes()

    println("-- counts")
    votes.like("u1", "a1")
    votes.like("u2", "a1")
    votes.dislike("u3", "a1")
    votes.like("u1", "a1")
    println("  a1 likes=${votes.likes("a1")} dislikes=${votes.dislikes("a1")}")

    println("-- switching moves the count")
    votes.dislike("u1", "a1")
    println("  a1 likes=${votes.likes("a1")} dislikes=${votes.dislikes("a1")}")

    println("-- last 3 switches for u1, newest first")
    votes.like("u1", "a1")
    votes.like("u1", "a2")
    votes.dislike("u1", "a2")
    votes.like("u1", "a2")
    for (switch in votes.recentSwitches("u1")) {
        println("  ${switch.articleId}: ${switch.from} -> ${switch.to}")
    }

    println("-- clear a vote")
    votes.clear("u2", "a1")
    println("  a1 likes=${votes.likes("a1")} dislikes=${votes.dislikes("a1")}")
}
