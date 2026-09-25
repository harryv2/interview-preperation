package lld.quora

import lld.quora.entity.CommentTargetType
import lld.quora.entity.VotableType
import lld.quora.entity.VoteValue
import lld.quora.service.QuoraService
import lld.quora.strategy.ByRatioRanking

fun main() {
    val service = QuoraService()

    val aaryan = service.registerUser("Aaryan", "aaryan@x.com")
    val ravi = service.registerUser("Ravi", "ravi@x.com")
    val sunil = service.registerUser("Sunil", "sunil@x.com")
    val priya = service.registerUser("Priya", "priya@x.com")
    val meena = service.registerUser("Meena", "meena@x.com")

    val swe = service.createTopic("Software Engineering")
    val career = service.createTopic("Career Advice")

    println("=== ask, tagged with two topics ===")
    val question = service.askQuestion(
        author = aaryan,
        title = "Best way to learn system design?",
        body = "Preparing for SDE-2 rounds. Where should I start?",
        topicList = listOf(swe, career)
    )
    println("  $question")

    println("\n=== two answers ===")
    val raviAnswer = service.postAnswer(question.id, ravi,
        "Start with LLD. Do five problems end to end before touching HLD.")
    val sunilAnswer = service.postAnswer(question.id, sunil,
        "Read the Grokking course and take notes.")

    println("\n=== votes ===")
    service.vote(priya, VotableType.ANSWER, raviAnswer.id, VoteValue.UP)
    service.vote(meena, VotableType.ANSWER, raviAnswer.id, VoteValue.UP)
    service.vote(aaryan, VotableType.ANSWER, raviAnswer.id, VoteValue.UP)
    service.vote(priya, VotableType.ANSWER, sunilAnswer.id, VoteValue.UP)
    println("  Ravi: ${raviAnswer.score()}, Sunil: ${sunilAnswer.score()}")

    println("\n=== Priya toggles her upvote off ===")
    println("  " + service.vote(priya, VotableType.ANSWER, raviAnswer.id, VoteValue.UP))
    println("  Ravi score: ${raviAnswer.score()}")

    println("\n=== Priya downvotes — swing of 2, not 1 ===")
    service.vote(priya, VotableType.ANSWER, raviAnswer.id, VoteValue.UP)
    println("  after re-upvote: ${raviAnswer.score()}")
    println("  " + service.vote(priya, VotableType.ANSWER, raviAnswer.id, VoteValue.DOWN))
    println("  after switch: ${raviAnswer.score()} " +
            "(${raviAnswer.upvotes}↑ ${raviAnswer.downvotes}↓)")

    println("\n=== comment thread with flattening ===")
    val meenaComment = service.commentOn(CommentTargetType.ANSWER, raviAnswer.id,
        meena, "Which five problems would you pick?")
    val raviReply = service.replyTo(meenaComment.id, ravi,
        "Elevator, parking lot, Splitwise, BookMyShow, Uber.")
    // A reply to a reply stays at level one.
    service.replyTo(raviReply.id, priya, "Add a bidding system to that list.")

    service.commentsOn(CommentTargetType.ANSWER, raviAnswer.id).forEach { println(it) }

    println("\n=== ranked by score ===")
    service.answersFor(question.id).forEach { println("  $it") }

    println("\n=== ranked by ratio — small clean answer wins ===")
    service.setRanking(ByRatioRanking())
    service.answersFor(question.id).forEach { println("  $it") }

    println("\n=== cannot vote on your own content ===")
    try {
        service.vote(ravi, VotableType.ANSWER, raviAnswer.id, VoteValue.UP)
    } catch (e: Exception) {
        println("  rejected: ${e.message}")
    }

    println("\n=== topic page ===")
    service.questionsUnder(swe.id).forEach { println("  $it") }

    println("\n=== profiles ===")
    println("  " + service.profileOf(ravi))
    println("  " + service.profileOf(aaryan))

    println("\n=== reconciliation ===")
    val drift = service.runReconciliation()
    println("  mismatches: ${drift.size}")
}