package lld_self.splitwise.entity

import kotlin.uuid.Uuid

class Splitwise {

    val users = HashMap<Uuid, User>();
    var groups = HashMap<Uuid, Group>();


    fun createUser(name: String): User {
        var id = Uuid.random()
        users[id] = User(id, name)
        return users[id]!!
    }

    fun createGroup(name: String, members: List<Uuid>, simplified: Boolean): Group {
        members.forEach {
            require(users.contains(it)) {"User $it not found"}
        }

        val members = members.map { users[it]!!}

        var groupId = Uuid.random()
        val group = Group(groupId, name, members.toTypedArray(), simplified)

        groups[groupId] = group
        return group
    }


    fun getSummary(userId: Uuid): Money {
        require(users.contains(userId)) {"User $userId not found"}

        var userMoney = Money.ZERO

        for(group in groups) {
            if(!group.value.memberIds.contains(userId)) {
                continue
            }

            userMoney += group.value.getBalance(users[userId]!!)
        }

        return userMoney
    }

}