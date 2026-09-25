package lld.media.musicplayer.entity

class Playlist(val id: String, val name: String) {
    private val songs = ArrayList<Song>()

    val size: Int
        get() = songs.size

    fun add(song: Song) {
        songs += song
    }

    fun remove(songId: String) {
        songs.removeIf { it.id == songId }
    }

    fun songAt(index: Int): Song {
        return songs[index]
    }

    fun all(): List<Song> {
        return songs.toList()
    }
}
