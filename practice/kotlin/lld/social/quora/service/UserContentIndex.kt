package lld.social.quora.service

import lld.social.quora.entity.VotableType
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// reverse lookups for the profile page, one write path so it cannot drift
class UserContentIndex {

    private val lock = ReentrantLock()
    private val contentByUser = mutableMapOf<String, MutableList<String>>()

    fun record(userId: String, type: VotableType, contentId: String) {
        lock.withLock {
            contentByUser.getOrPut(key(userId, type)) { mutableListOf() }.add(contentId)
        }
    }

    fun contentBy(userId: String, type: VotableType): List<String> {
        lock.withLock {
            return contentByUser[key(userId, type)].orEmpty().toList()
        }
    }

    private fun key(userId: String, type: VotableType): String {
        return "$userId:$type"
    }
}
