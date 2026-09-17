package lld_self.splitwise.entity

import kotlin.uuid.Uuid

class Splitwise {

    private val users = HashMap<Uuid, User>()
    private val groups = HashMap<Uuid, Group>()


    fun createUser(name: String): User {
        val user = User(Uuid.random(), name)
        users[user.id] = user
        return user
    }

    fun createGroup(name: String, memberIds: List<Uuid>, simplified: Boolean): Group {
        val members = memberIds.map {
            requireNotNull(users[it]) { "User $it not found" }
        }

        val group = Group(Uuid.random(), name, members.toSet(), simplified)

        groups[group.id] = group
        return group
    }


    fun getSummary(userId: Uuid): Money {
        val user = requireNotNull(users[userId]) { "User $userId not found" }

        var userMoney = Money.ZERO

        for (group in groups.values) {
            if (user !in group.members) {
                continue
            }

            userMoney += group.getBalance(user)
        }

        return userMoney
    }

}
