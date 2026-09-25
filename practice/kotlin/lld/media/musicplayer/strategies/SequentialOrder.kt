package lld.media.musicplayer.strategies

class SequentialOrder : PlayOrder {
    override fun reset(size: Int) {
    }

    override fun songIndex(position: Int): Int {
        return position
    }
}
