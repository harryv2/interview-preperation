package lld.social.quora.service

import lld.social.quora.entity.Answer
import lld.social.quora.entity.Comment
import lld.social.quora.entity.CommentTargetType
import lld.social.quora.entity.Content
import lld.social.quora.entity.Question
import lld.social.quora.entity.Topic
import lld.social.quora.entity.User
import lld.social.quora.entity.UserStats
import lld.social.quora.entity.VotableType
import lld.social.quora.entity.VoteOutcome
import lld.social.quora.entity.VoteValue
import lld.social.quora.strategy.AnswerRankingStrategy
import lld.social.quora.strategy.ByScoreRanking
import java.time.Instant
import java.util.UUID


// the entry point, owns the wiring and not the rules
class QuoraService(private var ranking: AnswerRankingStrategy = ByScoreRanking()) {

    private val users = mutableMapOf<String, User>()
    private val stats = mutableMapOf<String, UserStats>()
    private val questions = mutableMapOf<String, Question>()
    private val answers = mutableMapOf<String, Answer>()
    private val comments = mutableMapOf<String, Comment>()
    private val topics = mutableMapOf<String, Topic>()

    private val userIndex = UserContentIndex()

    private val voteService = VoteService(
        contentLookup = { type, id -> lookupContent(type, id) },
        statsFor = { userId -> statsFor(userId) }
    )

    fun registerUser(name: String, email: String): User {
        val user = User(UUID.randomUUID().toString(), name, email, Instant.now())
        users[user.id] = user
        stats[user.id] = UserStats()
        return user
    }

    fun createTopic(name: String): Topic {
        val topic = Topic(UUID.randomUUID().toString(), name)
        topics[topic.id] = topic
        return topic
    }

    fun askQuestion(author: User, title: String, body: String, topicList: List<Topic>): Question {
        require(title.isNotBlank()) { "Title cannot be empty" }

        val question = Question(UUID.randomUUID().toString(), author.id, title, body, Instant.now())
        questions[question.id] = question

        // one method maintains both sides of the topic relationship
        topicList.forEach {
            question.tagWith(it.id)
            it.addQuestion(question.id)
        }

        recordAuthored(author.id, VotableType.QUESTION, question.id)
        return question
    }

    fun postAnswer(questionId: String, author: User, body: String): Answer {
        val question = questions[questionId]
        requireNotNull(question) { "No question with id $questionId" }
        require(body.isNotBlank()) { "Answer cannot be empty" }

        val answer = Answer(UUID.randomUUID().toString(), author.id, body, Instant.now(), questionId)

        // the question decides whether it will accept this
        question.addAnswer(answer.id)
        answers[answer.id] = answer

        recordAuthored(author.id, VotableType.ANSWER, answer.id)
        return answer
    }

    fun commentOn(targetType: CommentTargetType, targetId: String, author: User, body: String): Comment {
        val exists = when (targetType) {
            CommentTargetType.QUESTION -> questions.containsKey(targetId)
            CommentTargetType.ANSWER -> answers.containsKey(targetId)
        }
        require(exists) { "No $targetType with id $targetId" }
        require(body.isNotBlank()) { "Comment cannot be empty" }

        val comment = Comment(
            id = UUID.randomUUID().toString(),
            authorId = author.id,
            body = body,
            createdAt = Instant.now(),
            targetType = targetType,
            targetId = targetId,
            parentCommentId = null,
            replyingToUserId = null
        )
        comments[comment.id] = comment

        recordAuthored(author.id, VotableType.COMMENT, comment.id)
        return comment
    }

    fun replyTo(commentId: String, author: User, body: String): Comment {
        val parent = comments[commentId]
        requireNotNull(parent) { "No comment with id $commentId" }

        // the comment builds its own reply, applying the flattening rule
        val reply = parent.buildReply(author.id, body)
        comments[reply.id] = reply

        recordAuthored(author.id, VotableType.COMMENT, reply.id)
        return reply
    }

    fun vote(voter: User, type: VotableType, targetId: String, value: VoteValue): VoteOutcome {
        return voteService.castVote(voter.id, type, targetId, value)
    }

    fun myVote(voter: User, type: VotableType, targetId: String): VoteValue? {
        return voteService.voteOf(voter.id, type, targetId)
    }

    fun setRanking(strategy: AnswerRankingStrategy) {
        ranking = strategy
    }

    fun answersFor(questionId: String): List<Answer> {
        val question = questions[questionId] ?: return emptyList()
        val live = question.answerIds()
            .mapNotNull { answers[it] }
            .filter { !it.isDeleted }
        return ranking.rank(live)
    }

    fun commentsOn(targetType: CommentTargetType, targetId: String): List<Comment> {
        val relevant = comments.values.filter {
            it.targetType == targetType && it.targetId == targetId && !it.isDeleted
        }
        val repliesByParent = relevant.filter { !it.isTopLevel() }.groupBy { it.parentCommentId }

        // top level ordered by score, each followed by its replies in the order they were written
        return relevant
            .filter { it.isTopLevel() }
            .sortedByDescending { it.score() }
            .flatMap { parent ->
                listOf(parent) + repliesByParent[parent.id].orEmpty().sortedBy { it.createdAt }
            }
    }

    fun questionsUnder(topicId: String): List<Question> {
        val topic = topics[topicId] ?: return emptyList()
        return topic.questionIds()
            .mapNotNull { questions[it] }
            .filter { !it.isDeleted }
            .sortedByDescending { it.score() }
    }

    fun questionsBy(userId: String): List<Question> {
        return userIndex.contentBy(userId, VotableType.QUESTION).mapNotNull { questions[it] }
    }

    fun answersBy(userId: String): List<Answer> {
        return userIndex.contentBy(userId, VotableType.ANSWER).mapNotNull { answers[it] }
    }

    fun commentsBy(userId: String): List<Comment> {
        return userIndex.contentBy(userId, VotableType.COMMENT).mapNotNull { comments[it] }
    }

    fun profileOf(user: User): String {
        val userStats = statsFor(user.id)
        return "${user.name} — ${userStats.questionCount} questions, " +
            "${userStats.answerCount} answers, " +
            "${userStats.commentCount} comments, " +
            "reputation ${userStats.reputation()}"
    }

    fun runReconciliation(): List<String> {
        val all = buildList<Content> {
            addAll(questions.values)
            addAll(answers.values)
            addAll(comments.values)
        }
        return voteService.reconcile(all)
    }

    private fun recordAuthored(userId: String, type: VotableType, contentId: String) {
        userIndex.record(userId, type, contentId)

        val userStats = statsFor(userId)
        when (type) {
            VotableType.QUESTION -> userStats.recordQuestion()
            VotableType.ANSWER -> userStats.recordAnswer()
            VotableType.COMMENT -> userStats.recordComment()
        }
    }

    private fun lookupContent(type: VotableType, id: String): Content? {
        return when (type) {
            VotableType.QUESTION -> questions[id]
            VotableType.ANSWER -> answers[id]
            VotableType.COMMENT -> comments[id]
        }
    }

    private fun statsFor(userId: String): UserStats {
        return stats.getOrPut(userId) { UserStats() }
    }
}
