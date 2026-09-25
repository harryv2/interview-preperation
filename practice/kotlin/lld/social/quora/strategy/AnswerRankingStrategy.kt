package lld.social.quora.strategy

import lld.social.quora.entity.Answer


interface AnswerRankingStrategy {
    val name: String
    fun rank(answers: List<Answer>): List<Answer>
}


class ByScoreRanking : AnswerRankingStrategy {

    override val name = "top"

    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending { it.score() }
    }
}


class ByNewestRanking : AnswerRankingStrategy {

    override val name = "newest"

    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending { it.createdAt }
    }
}


// wilson-ish: an answer with 10 up 0 down beats one with 100 up 95 down, even though the second has a higher raw score
class ByRatioRanking : AnswerRankingStrategy {

    override val name = "ratio"

    override fun rank(answers: List<Answer>): List<Answer> {
        return answers.sortedByDescending {
            val total = it.upvotes + it.downvotes
            if (total == 0) 0.0 else it.upvotes.toDouble() / total
        }
    }
}
