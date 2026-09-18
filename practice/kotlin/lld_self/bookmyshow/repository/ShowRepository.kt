package lld_self.bookmyshow.repository

import lld_self.bookmyshow.entities.Show
import kotlin.uuid.Uuid


interface ShowRepository : Repository<Show, Uuid> {
    fun findByTheater(theaterId: Uuid): List<Show>
    fun findByScreen(screenId: Uuid): List<Show>
}


class InMemoryShowRepository : InMemoryRepository<Show, Uuid>({ it.id }), ShowRepository {

    override fun findByTheater(theaterId: Uuid): List<Show> {
        return store.values.filter { it.screen.theaterId == theaterId }
    }

    override fun findByScreen(screenId: Uuid): List<Show> {
        return store.values.filter { it.screen.id == screenId }
    }
}
