package lld.quora.service

import lld.quora.entity.Content
import lld.quora.entity.UserStats
import lld.quora.entity.VotableType
import lld.quora.entity.Vote
import lld.quora.entity.VoteOutcome
import lld.quora.entity.VoteValue
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// owns the vote map and the lock, the composite key is what makes toggling O(1)
class VoteService(
    private val contentLookup: (VotableType, String) -> Content?,
    private val statsFor: (String) -> UserStats
) {

    private val lock = ReentrantLock()
    private val votes = mutableMapOf<String, Vote>()

    fun castVote(voterId: String, type: VotableType, targetId: String, value: VoteValue): VoteOutcome {
        lock.withLock {
            val target = contentLookup(type, targetId)
            requireNotNull(target) { "No $type with id $targetId" }
            require(!target.isDeleted) { "Cannot vote on deleted content" }
            require(target.authorId != voterId) { "Cannot vote on your own content" }

            val key = voteKey(voterId, type, targetId)
            val existing = votes[key]
            val authorStats = statsFor(target.authorId)

            if (existing == null) {
                votes[key] = Vote(UUID.randomUUID().toString(), voterId, type, targetId, value, Instant.now())
                target.applyVote(value, 1)
                authorStats.recordVoteReceived(value, 1)
                return VoteOutcome.ADDED
            }

            if (existing.value == value) {
                votes.remove(key)
                target.applyVote(value, -1)
                authorStats.recordVoteReceived(value, -1)
                return VoteOutcome.REMOVED
            }

            // switched, a net swing of two and not one
            votes[key] = Vote(existing.id, voterId, type, targetId, value, Instant.now())
            target.applyVote(existing.value, -1)
            target.applyVote(value, 1)
            authorStats.recordVoteReceived(existing.value, -1)
            authorStats.recordVoteReceived(value, 1)
            return VoteOutcome.SWITCHED
        }
    }

    fun voteOf(voterId: String, type: VotableType, targetId: String): VoteValue? {
        lock.withLock {
            return votes[voteKey(voterId, type, targetId)]?.value
        }
    }

    // recounts stored tallies from the vote records, run periodically to catch drift between the two copies of the truth
    fun reconcile(allContent: List<Content>): List<String> {
        lock.withLock {
            val castOn = votes.values.groupBy { it.targetId }

            return allContent.mapNotNull { content ->
                val cast = castOn[content.id].orEmpty()
                val up = cast.count { it.value == VoteValue.UP }
                val down = cast.size - up

                if (up == content.upvotes && down == content.downvotes) {
                    null
                } else {
                    "${content.id}: stored ${content.upvotes}/${content.downvotes}, actual $up/$down"
                }
            }
        }
    }

    private fun voteKey(voterId: String, type: VotableType, targetId: String): String {
        return "$voterId:$type:$targetId"
    }
}
