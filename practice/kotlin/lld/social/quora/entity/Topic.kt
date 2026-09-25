package lld.social.quora.entity


// bidirectional index with Question, earns the duplication because a topic page would otherwise scan every question
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
