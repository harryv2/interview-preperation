package lld_self.bookmyshow.repository

import lld_self.bookmyshow.entities.Theater
import kotlin.uuid.Uuid


interface TheaterRepository : Repository<Theater, Uuid> {
    fun findByCity(city: String): List<Theater>
}


class InMemoryTheaterRepository : InMemoryRepository<Theater, Uuid>({ it.id }), TheaterRepository {

    override fun findByCity(city: String): List<Theater> {
        return store.values.filter { it.city == city }
    }
}
