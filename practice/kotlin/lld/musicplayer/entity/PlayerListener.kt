package lld.musicplayer.entity

fun interface PlayerListener {
    fun onChange(state: PlaybackState, song: Song?)
}
