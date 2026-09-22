package lld.deliveryslot.service

import lld.deliveryslot.entity.TimeSlot
import java.time.Clock
import java.time.LocalDate

data class SlotConfig(
    val startHour: Int = 9,
    val endHour: Int = 21,
    val durationHours: Int = 2,
    val horizonDays: Int = 4
)

class SlotCatalog(
    private val config: SlotConfig,
    private val clock: Clock
) {
    fun slotsFor(warehouseId: String): List<TimeSlot> {
        val today = LocalDate.now(clock)
        val slots = mutableListOf<TimeSlot>()

        for (day in 0 until config.horizonDays) {
            val date = today.plusDays(day.toLong())
            var hour = config.startHour
            while (hour + config.durationHours <= config.endHour) {
                slots.add(
                    TimeSlot(
                        id = slotId(warehouseId, date, hour),
                        warehouseId = warehouseId,
                        start = date.atTime(hour, 0),
                        end = date.atTime(hour + config.durationHours, 0)
                    )
                )
                hour += config.durationHours
            }
        }
        return slots
    }

    fun findSlot(warehouseId: String, slotId: String): TimeSlot? {
        return slotsFor(warehouseId).firstOrNull { it.id == slotId }
    }

    private fun slotId(warehouseId: String, date: LocalDate, hour: Int): String {
        return "$warehouseId:$date:$hour"
    }
}
