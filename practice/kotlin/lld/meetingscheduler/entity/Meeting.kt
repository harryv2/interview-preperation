package lld.meetingscheduler.entity

import kotlin.uuid.Uuid

data class Meeting(
    val id: Uuid,
    val title: String,
    val organizer: User,
    val attendees: Set<User>,
    val room: Room,
    val slot: TimeSlot,
)
