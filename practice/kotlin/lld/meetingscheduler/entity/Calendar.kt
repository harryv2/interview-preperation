package lld.meetingscheduler.entity

import kotlin.time.Instant
import kotlin.uuid.Uuid

class Calendar {
    private val meetings = LinkedHashMap<Uuid, Meeting>()

    fun isFree(slot: TimeSlot): Boolean {
        return meetings.values.none { it.slot.overlaps(slot) }
    }

    fun meetingAt(instant: Instant): Meeting? {
        return meetings.values.firstOrNull { instant >= it.slot.start && instant < it.slot.end }
    }

    fun add(meeting: Meeting) {
        meetings[meeting.id] = meeting
    }

    fun remove(meetingId: Uuid) {
        meetings.remove(meetingId)
    }

    fun all(): List<Meeting> {
        return meetings.values.sortedBy { it.slot.start }
    }
}
