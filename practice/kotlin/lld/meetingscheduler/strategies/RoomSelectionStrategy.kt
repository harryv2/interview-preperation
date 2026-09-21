package lld.meetingscheduler.strategies

import lld.meetingscheduler.entity.Room

fun interface RoomSelectionStrategy {
    fun select(freeRooms: List<Room>, headcount: Int): Room?
}
