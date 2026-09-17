package lld_self.splitwise.entity

import java.util.PriorityQueue


interface DebtSimplifier{
    fun simplify(balances: Map<User, Money>): List<Transfer>
}

class GreedySimplifier: DebtSimplifier {

    private data class UserOwned(val user: User, val amount: Money)

    override fun simplify(balances: Map<User, Money>): List<Transfer> {
        var whoHaveGiven = PriorityQueue<UserOwned>(compareBy { it.amount })
        var whoOwes = PriorityQueue<UserOwned>(compareBy { it.amount })

        for(entry in balances) {
            if(entry.value < Money.ZERO) {
                whoOwes.add(UserOwned(entry.key, entry.value))
            } else {
                whoHaveGiven.add(UserOwned(entry.key, entry.value))
            }
        }

        var transfers = mutableListOf<Transfer>()

        while (whoOwes.isNotEmpty() && whoHaveGiven.isNotEmpty()) {
            var owes = whoOwes.poll()
            var gets = whoHaveGiven.poll()

            val diff = gets.amount - owes.amount

            if(diff > Money.ZERO) {
                transfers.add(Transfer(owes.user, gets.user, owes.amount))
                whoHaveGiven.add(UserOwned(gets.user, diff))
            } else if(diff == Money.ZERO) {
                transfers.add(Transfer(owes.user, gets.user, owes.amount))
            } else {
                transfers.add(Transfer(owes.user, gets.user, gets.amount))
                whoOwes.add(UserOwned(owes.user, owes.amount + diff))
            }

        }

        return transfers
    }


}