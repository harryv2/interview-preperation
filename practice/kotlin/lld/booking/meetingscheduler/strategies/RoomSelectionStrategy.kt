package lld.booking.meetingscheduler.strategies

import lld.booking.meetingscheduler.entity.Room

fun interface RoomSelectionStrategy {
    fun select(freeRooms: List<Room>, headcount: Int): Room?
}
