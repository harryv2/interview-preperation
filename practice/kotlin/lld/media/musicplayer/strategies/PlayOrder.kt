package lld.media.musicplayer.strategies

interface PlayOrder {
    fun reset(size: Int)

    fun songIndex(position: Int): Int
}
