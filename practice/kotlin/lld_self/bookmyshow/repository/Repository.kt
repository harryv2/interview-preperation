package lld_self.bookmyshow.repository

import java.util.concurrent.ConcurrentHashMap


interface Repository<T, ID> {
    fun save(entity: T): T
    fun findById(id: ID): T?
    fun findAll(): List<T>
}


abstract class InMemoryRepository<T, ID>(private val idOf: (T) -> ID) : Repository<T, ID> {

    protected val store = ConcurrentHashMap<ID, T>()

    override fun save(entity: T): T {
        store[idOf(entity)] = entity
        return entity
    }

    override fun findById(id: ID): T? {
        return store[id]
    }

    override fun findAll(): List<T> {
        return store.values.toList()
    }
}
