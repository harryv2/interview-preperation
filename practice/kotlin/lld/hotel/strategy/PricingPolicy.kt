package lld.hotel.strategy

import lld.hotel.entity.DateRange
import lld.hotel.entity.Money
import lld.hotel.entity.RoomType
import java.time.DayOfWeek


// The one seam. What a night costs is the thing a hotel actually changes: weekends, seasons, length of stay.
// It prices a TYPE, because at booking time no particular room has been picked yet.
interface PricingPolicy {
    fun quote(type: RoomType, stay: DateRange): Money
}


class FlatRate : PricingPolicy {
    override fun quote(type: RoomType, stay: DateRange): Money {
        return type.baseRate * stay.nights
    }
}


// priced per night rather than per stay, which is why DateRange hands out its dates
class WeekendSurcharge(private val percent: Int) : PricingPolicy {

    override fun quote(type: RoomType, stay: DateRange): Money {
        return stay.dates().fold(Money.ZERO) { total, night ->
            total + if (night.dayOfWeek in WEEKEND) type.baseRate.percent(100 + percent) else type.baseRate
        }
    }

    companion object {
        private val WEEKEND = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
    }
}
