package lld_self.splitwise.entity

import java.util.PriorityQueue


interface DebtSimplifier{
    fun simplify(balances: Map<User, Money>): List<Transfer>
}

class GreedySimplifier: DebtSimplifier {

    private data class UserAmount(val user: User, val amount: Money)

    override fun simplify(balances: Map<User, Money>): List<Transfer> {
        val creditors = PriorityQueue<UserAmount>(compareBy { it.amount })
        val debtors = PriorityQueue<UserAmount>(compareBy { it.amount })

        for(entry in balances) {
            if(entry.value < Money.ZERO) {
                // store the debt as a positive amount so the arithmetic below just works
                debtors.add(UserAmount(entry.key, -entry.value))
            } else if(entry.value > Money.ZERO) {
                creditors.add(UserAmount(entry.key, entry.value))
            }
        }

        val transfers = mutableListOf<Transfer>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val owes = debtors.poll()
            val gets = creditors.poll()

            val diff = gets.amount - owes.amount

            if(diff > Money.ZERO) {
                transfers.add(Transfer(owes.user, gets.user, owes.amount))
                creditors.add(UserAmount(gets.user, diff))
            } else if(diff == Money.ZERO) {
                transfers.add(Transfer(owes.user, gets.user, owes.amount))
            } else {
                transfers.add(Transfer(owes.user, gets.user, gets.amount))
                debtors.add(UserAmount(owes.user, -diff))
            }

        }

        return transfers
    }


}
