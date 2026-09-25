package lld.media.musicplayer.entity

import lld.media.musicplayer.strategies.PlayOrder
import lld.media.musicplayer.strategies.SequentialOrder

class Player {
    private val listeners = ArrayList<PlayerListener>()
    private var playlist: Playlist? = null
    private var order: PlayOrder = SequentialOrder()
    private var position = 0

    var state = PlaybackState.STOPPED
        private set

    var repeat = RepeatMode.OFF

    val current: Song?
        get() {
            val list = playlist ?: return null
            if (list.size == 0) return null
            return list.songAt(order.songIndex(position))
        }

    fun addListener(listener: PlayerListener) {
        listeners += listener
    }

    fun load(playlist: Playlist) {
        this.playlist = playlist
        order.reset(playlist.size)
        position = 0
        state = PlaybackState.STOPPED
        notifyListeners()
    }

    fun setOrder(order: PlayOrder) {
        this.order = order
        order.reset(playlist?.size ?: 0)
        position = 0
    }

    fun play() {
        checkNotNull(current) { "Nothing loaded" }
        state = PlaybackState.PLAYING
        notifyListeners()
    }

    fun pause() {
        if (state != PlaybackState.PLAYING) return
        state = PlaybackState.PAUSED
        notifyListeners()
    }

    fun stop() {
        state = PlaybackState.STOPPED
        position = 0
        notifyListeners()
    }

    fun next() {
        val size = playlist?.size ?: return
        if (repeat == RepeatMode.ONE) {
            notifyListeners()
            return
        }

        if (position + 1 < size) {
            position++
        } else if (repeat == RepeatMode.ALL) {
            order.reset(size)
            position = 0
        } else {
            stop()
            return
        }
        notifyListeners()
    }

    fun previous() {
        if (position > 0) {
            position--
        }
        notifyListeners()
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            listener.onChange(state, current)
        }
    }
}
