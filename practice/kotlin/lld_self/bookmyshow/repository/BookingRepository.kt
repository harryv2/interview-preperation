package lld_self.bookmyshow.repository

import lld_self.bookmyshow.entities.Booking
import lld_self.bookmyshow.entities.BookingStatus
import kotlin.uuid.Uuid


interface BookingRepository : Repository<Booking, Uuid> {
    fun findByStatus(status: BookingStatus): List<Booking>
}


class InMemoryBookingRepository : InMemoryRepository<Booking, Uuid>({ it.id }), BookingRepository {

    override fun findByStatus(status: BookingStatus): List<Booking> {
        return store.values.filter { it.status == status }
    }
}
