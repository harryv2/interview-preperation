package lld.quora

import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

// ===============================================================
// Enums
// ===============================================================

enum class VoteValue(val delta: Int) {
    UP(1),
    DOWN(-1)
}

enum class VotableType { QUESTION, ANSWER, COMMENT }

enum class CommentTargetType { QUESTION, ANSWER }

enum class VoteOutcome { ADDED, REMOVED, SWITCHED }

// ===============================================================
// User — a plain record. Reverse lookups live in an index.
// ===============================================================

class User(
    val id: String,
    val name: String,
    val email: String,
    val joinedAt: Instant
)

// Stored, not derived: counting every vote on everything a person
// ever wrote, on every profile load, does not scale.
class UserStats {

    var questionCount: Int = 0
        private set
    var answerCount: Int = 0
        private set
    var commentCount: Int = 0
        private set
    var upvotesReceived: Int = 0
        private set
    var downvotesReceived: Int = 0
        private set

    fun recordQuestion() {
        questionCount = questionCount + 1
    }

    fun recordAnswer() {
        answerCount = answerCount + 1
    }

    fun recordComment() {
        commentCount = commentCount + 1
    }

    fun recordVoteReceived(value: VoteValue, direction: Int) {
        if (value == VoteValue.UP) {
            upvotesReceived = upvotesReceived + direction
        } else {
            downvotesReceived = downvotesReceived + direction
        }
    }

    // A method, not a field. Weights change; you don't want to
    // recompute a stored number across every user when they do.
    fun reputation(): Int {
        return upvotesReceived * 10 - downvotesReceived * 2
    }
}

// ===============================================================
// Content — the base. This is a base class rather than an interface
// because the vote-counting IMPLEMENTATION is shared, not just
// the contract.
// ===============================================================

abstract class Content(
    val id: String,
    val authorId: String,
    body: String,
    val createdAt: Instant
) {

    var body: String = body
        private set

    var upvotes: Int = 0
        private set

    var downvotes: Int = 0
        private set

    var isDeleted: Boolean = false
        private set

    abstract fun votableType(): VotableType

    fun score(): Int {
        return upvotes - downvotes
    }

    // Only the vote service calls this, always under its lock.
    internal fun applyVote(value: VoteValue, direction: Int) {
        if (value == VoteValue.UP) {
            upvotes = upvotes + direction
        } else {
            downvotes = downvotes + direction
        }
    }

    fun edit(newBody: String, editorId: String) {
        require(editorId == authorId) { "Only the author can edit" }
        require(!isDeleted) { "Cannot edit deleted content" }
        require(newBody.isNotBlank()) { "Body cannot be empty" }
        body = newBody
    }

    fun softDelete(requesterId: String) {
        require(requesterId == authorId) { "Only the author can delete" }
        isDeleted = true
    }
}

// ===============================================================
// Question — owns its own collections and the rules over them
// ===============================================================

class Question(
    id: String,
    authorId: String,
    val title: String,
    body: String,
    createdAt: Instant
) : Content(id, authorId, body, createdAt) {

    companion object {
        const val MAX_TOPICS = 5
    }

    private val topicIds = mutableSetOf<String>()
    private val answerIds = mutableListOf<String>()

    var isClosed: Boolean = false
        private set

    override fun votableType(): VotableType {
        return VotableType.QUESTION
    }

    // The question enforces its own rules. Nothing outside can
    // reach into answerIds and skip the check.
    fun addAnswer(answerId: String) {
        check(!isClosed) { "Cannot answer a closed question" }
        check(!isDeleted) { "Cannot answer a deleted question" }
        answerIds.add(answerId)
    }

    fun tagWith(topicId: String) {
        check(topicIds.size < MAX_TOPICS) {
            "A question can carry at most $MAX_TOPICS topics"
        }
        topicIds.add(topicId)
    }

    fun untag(topicId: String) {
        topicIds.remove(topicId)
    }

    fun close(requesterId: String) {
        require(requesterId == authorId) { "Only the author can close" }
        isClosed = true
    }

    fun answerIds(): List<String> {
        return answerIds.toList()
    }

    fun topicIds(): Set<String> {
        return topicIds.toSet()
    }

    fun answerCount(): Int {
        return answerIds.size
    }

    override fun toString(): String {
        return "Q(${id.take(6)}) \"$title\" score=${score()} answers=${answerIds.size}"
    }
}

// ===============================================================
// Answer
// ===============================================================

class Answer(
    id: String,
    authorId: String,
    body: String,
    createdAt: Instant,
    val questionId: String
) : Content(id, authorId, body, createdAt) {

    override fun votableType(): VotableType {
        return VotableType.ANSWER
    }

    override fun toString(): String {
        val preview = if (body.length > 40) body.take(40) + "..." else body
        return "A(${id.take(6)}) \"$preview\" ${upvotes}↑ ${downvotes}↓ score=${score()}"
    }
}

// ===============================================================
// Comment — carries the two-level flattening rule itself
// ===============================================================

class Comment(
    id: String,
    authorId: String,
    body: String,
    createdAt: Instant,
    val targetType: CommentTargetType,
    val targetId: String,
    val parentCommentId: String?,
    val replyingToUserId: String?
) : Content(id, authorId, body, createdAt) {

    override fun votableType(): VotableType {
        return VotableType.COMMENT
    }

    fun isTopLevel(): Boolean {
        return parentCommentId == null
    }

    fun threadRoot(): String {
        return parentCommentId ?: id
    }

    // The comment knows the flattening rule. A reply to a reply
    // lands at level one and records who it is answering.
    fun buildReply(authorId: String, body: String): Comment {
        check(!isDeleted) { "Cannot reply to a deleted comment" }
        require(body.isNotBlank()) { "Reply cannot be empty" }

        return Comment(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            body = body,
            createdAt = Instant.now(),
            targetType = this.targetType,
            targetId = this.targetId,
            parentCommentId = this.threadRoot(),
            replyingToUserId = this.authorId
        )
    }

    override fun toString(): String {
        val prefix = if (isTopLevel()) "  " else "      ↳ "
        return "$prefix$body (score ${score()})"
    }
}

// ===============================================================
// Topic — bidirectional index with Question.
// Earns the duplication because a topic page would otherwise
// scan every question in the system.
// ===============================================================

class Topic(
    val id: String,
    val name: String
) {
    private val questionIds = mutableSetOf<String>()

    fun addQuestion(questionId: String) {
        questionIds.add(questionId)
    }

    fun removeQuestion(questionId: String) {
        questionIds.remove(questionId)
    }

    fun questionIds(): Set<String> {
        return questionIds.toSet()
    }

    fun questionCount(): Int {
        return questionIds.size
    }
}

// ===============================================================
// Vote — one class, polymorphic target
// ===============================================================

class Vote(
    val id: String,
    val voterId: String,
    val targetType: VotableType,
    val targetId: String,
    val value: VoteValue,
    val castAt: Instant
)

// ===============================================================
// Ordering — the one seam
// ===============================================================

interface AnswerRankingStrategy {
    fun rank(answers: List<Answer>): List<Answer>
    fun name(): String
}

class ByScoreRanking : AnswerRankingStrategy {
    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending { it.score() }
    }

    override fun name(): String {
        return "top"
    }
}

class ByNewestRanking : AnswerRankingStrategy {
    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending { it.createdAt }
    }

    override fun name(): String {
        return "newest"
    }
}

// Wilson-ish: an answer with 10↑ 0↓ beats one with 100↑ 95↓,
// even though the second has a higher raw score.
class ByRatioRanking : AnswerRankingStrategy {
    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending { answer ->
            val total = answer.upvotes + answer.downvotes
            if (total == 0) 0.0 else answer.upvotes.toDouble() / total
        }
    }

    override fun name(): String {
        return "ratio"
    }
}

// ===============================================================
// UserContentIndex — reverse lookups for the profile page.
// One write path, so it cannot drift.
// ===============================================================

class UserContentIndex {

    private val lock = ReentrantLock()

    private val questionsByUser = mutableMapOf<String, MutableList<String>>()
    private val answersByUser = mutableMapOf<String, MutableList<String>>()
    private val commentsByUser = mutableMapOf<String, MutableList<String>>()

    fun recordQuestion(userId: String, questionId: String) {
        lock.lock()
        try {
            questionsByUser.getOrPut(userId) { mutableListOf() }.add(questionId)
        } finally {
            lock.unlock()
        }
    }

    fun recordAnswer(userId: String, answerId: String) {
        lock.lock()
        try {
            answersByUser.getOrPut(userId) { mutableListOf() }.add(answerId)
        } finally {
            lock.unlock()
        }
    }

    fun recordComment(userId: String, commentId: String) {
        lock.lock()
        try {
            commentsByUser.getOrPut(userId) { mutableListOf() }.add(commentId)
        } finally {
            lock.unlock()
        }
    }

    fun questionsBy(userId: String): List<String> {
        lock.lock()
        try {
            return questionsByUser[userId]?.toList() ?: listOf()
        } finally {
            lock.unlock()
        }
    }

    fun answersBy(userId: String): List<String> {
        lock.lock()
        try {
            return answersByUser[userId]?.toList() ?: listOf()
        } finally {
            lock.unlock()
        }
    }

    fun commentsBy(userId: String): List<String> {
        lock.lock()
        try {
            return commentsByUser[userId]?.toList() ?: listOf()
        } finally {
            lock.unlock()
        }
    }
}

// ===============================================================
// VoteService — owns the vote map and the lock.
// The composite key is what makes toggling O(1).
// ===============================================================

class VoteService(
    private val contentLookup: (VotableType, String) -> Content?,
    private val statsFor: (String) -> UserStats
) {

    private val lock = ReentrantLock()
    private val votes = mutableMapOf<String, Vote>()

    private fun voteKey(voterId: String, type: VotableType, targetId: String): String {
        return "$voterId:$type:$targetId"
    }

    fun castVote(
        voterId: String,
        type: VotableType,
        targetId: String,
        value: VoteValue
    ): VoteOutcome {

        lock.lock()
        try {
            val target = contentLookup(type, targetId)
            require(target != null) { "No $type with id $targetId" }
            require(!target.isDeleted) { "Cannot vote on deleted content" }
            require(target.authorId != voterId) { "Cannot vote on your own content" }

            val key = voteKey(voterId, type, targetId)
            val existing = votes[key]
            val authorStats = statsFor(target.authorId)

            // No prior vote — add it.
            if (existing == null) {
                votes[key] = Vote(UUID.randomUUID().toString(), voterId,
                                  type, targetId, value, Instant.now())
                target.applyVote(value, 1)
                authorStats.recordVoteReceived(value, 1)
                return VoteOutcome.ADDED
            }

            // Same button again — toggle off.
            if (existing.value == value) {
                votes.remove(key)
                target.applyVote(value, -1)
                authorStats.recordVoteReceived(value, -1)
                return VoteOutcome.REMOVED
            }

            // Switched. Net swing of two, not one.
            votes[key] = Vote(existing.id, voterId, type, targetId, value, Instant.now())
            target.applyVote(existing.value, -1)
            target.applyVote(value, 1)
            authorStats.recordVoteReceived(existing.value, -1)
            authorStats.recordVoteReceived(value, 1)
            return VoteOutcome.SWITCHED
        } finally {
            lock.unlock()
        }
    }

    fun voteOf(voterId: String, type: VotableType, targetId: String): VoteValue? {
        lock.lock()
        try {
            return votes[voteKey(voterId, type, targetId)]?.value
        } finally {
            lock.unlock()
        }
    }

    // Recounts stored tallies from the vote records. Run periodically
    // to catch drift between the two copies of the truth.
    fun reconcile(allContent: List<Content>): List<String> {
        lock.lock()
        try {
            val mismatches = mutableListOf<String>()

            for (content in allContent) {
                var up = 0
                var down = 0
                for (vote in votes.values) {
                    if (vote.targetId == content.id) {
                        if (vote.value == VoteValue.UP) up++ else down++
                    }
                }
                if (up != content.upvotes || down != content.downvotes) {
                    mismatches.add("${content.id}: stored ${content.upvotes}/" +
                                   "${content.downvotes}, actual $up/$down")
                }
            }
            return mismatches
        } finally {
            lock.unlock()
        }
    }
}

// ===============================================================
// QuoraService — the entry point. Owns the wiring, not the rules.
// ===============================================================

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

    // ---------- users and topics ----------

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

    // ---------- content ----------

    fun askQuestion(author: User, title: String, body: String, topicList: List<Topic>): Question {
        require(title.isNotBlank()) { "Title cannot be empty" }

        val question = Question(UUID.randomUUID().toString(), author.id,
                                title, body, Instant.now())
        questions[question.id] = question

        // One method maintains BOTH sides of the topic relationship.
        for (topic in topicList) {
            question.tagWith(topic.id)
            topic.addQuestion(question.id)
        }

        userIndex.recordQuestion(author.id, question.id)
        statsFor(author.id).recordQuestion()
        return question
    }

    fun postAnswer(questionId: String, author: User, body: String): Answer {
        val question = questions[questionId] ?: error("No such question")
        require(body.isNotBlank()) { "Answer cannot be empty" }

        val answer = Answer(UUID.randomUUID().toString(), author.id,
                            body, Instant.now(), questionId)

        // The question decides whether it will accept this.
        question.addAnswer(answer.id)
        answers[answer.id] = answer

        userIndex.recordAnswer(author.id, answer.id)
        statsFor(author.id).recordAnswer()
        return answer
    }

    fun commentOn(
        targetType: CommentTargetType,
        targetId: String,
        author: User,
        body: String
    ): Comment {
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

        userIndex.recordComment(author.id, comment.id)
        statsFor(author.id).recordComment()
        return comment
    }

    fun replyTo(commentId: String, author: User, body: String): Comment {
        val parent = comments[commentId] ?: error("No such comment")

        // The comment builds its own reply, applying the flattening rule.
        val reply = parent.buildReply(author.id, body)
        comments[reply.id] = reply

        userIndex.recordComment(author.id, reply.id)
        statsFor(author.id).recordComment()
        return reply
    }

    // ---------- voting ----------

    fun vote(voter: User, type: VotableType, targetId: String, value: VoteValue): VoteOutcome {
        return voteService.castVote(voter.id, type, targetId, value)
    }

    fun myVote(voter: User, type: VotableType, targetId: String): VoteValue? {
        return voteService.voteOf(voter.id, type, targetId)
    }

    // ---------- reads ----------

    fun setRanking(strategy: AnswerRankingStrategy) {
        ranking = strategy
    }

    fun answersFor(questionId: String): List<Answer> {
        val question = questions[questionId] ?: return listOf()
        val live = question.answerIds()
            .mapNotNull { answers[it] }
            .filter { !it.isDeleted }
        return ranking.rank(live)
    }

    fun commentsOn(targetType: CommentTargetType, targetId: String): List<Comment> {
        val relevant = comments.values.filter {
            it.targetType == targetType && it.targetId == targetId && !it.isDeleted
        }

        // Top-level ordered by score, each followed by its replies
        // in the order they were written.
        val topLevel = relevant.filter { it.isTopLevel() }.sortedByDescending { it.score() }

        val result = mutableListOf<Comment>()
        for (parent in topLevel) {
            result.add(parent)
            val replies = relevant
                .filter { it.parentCommentId == parent.id }
                .sortedBy { it.createdAt }
            result.addAll(replies)
        }
        return result
    }

    fun questionsUnder(topicId: String): List<Question> {
        val topic = topics[topicId] ?: return listOf()
        return topic.questionIds()
            .mapNotNull { questions[it] }
            .filter { !it.isDeleted }
            .sortedByDescending { it.score() }
    }

    fun profileOf(user: User): String {
        val userStats = statsFor(user.id)
        return "${user.name} — ${userStats.questionCount} questions, " +
               "${userStats.answerCount} answers, " +
               "${userStats.commentCount} comments, " +
               "reputation ${userStats.reputation()}"
    }

    fun runReconciliation(): List<String> {
        val all = mutableListOf<Content>()
        all.addAll(questions.values)
        all.addAll(answers.values)
        all.addAll(comments.values)
        return voteService.reconcile(all)
    }
}