package lld.deliveryslot.repository

import lld.deliveryslot.entity.Customer

interface CustomerRepository {
    fun save(customer: Customer): Customer
    fun findById(id: String): Customer?
}

class InMemoryCustomerRepository : CustomerRepository {
    private val map = HashMap<String, Customer>()

    override fun save(customer: Customer): Customer {
        map[customer.id] = customer
        return customer
    }

    override fun findById(id: String): Customer? {
        return map[id]
    }
}
