package lld.rippling.musicanalytics

import lld.rippling.musicanalytics.entity.Song

class MusicPlayer(private val recentLimit: Int = 3) {
    private val songs = HashMap<Int, Song>()
    private val ranking = IndexedPriorityQueue<Int, Song>(
        compareByDescending<Song> { it.uniqueListens }.thenBy { it.title },
    ) { it.id }
    private val recentByUser = HashMap<String, LinkedHashSet<Int>>()
    private var nextId = 1

    fun addSong(title: String): Song {
        val song = Song(nextId++, title)
        songs[song.id] = song
        ranking.add(song)
        return song
    }

    fun play(songId: Int, userId: String) {
        val song = requireNotNull(songs[songId]) { "No song $songId" }
        if (song.listeners.add(userId)) {
            ranking.update(song)
        }

        val recent = recentByUser.getOrPut(userId) { LinkedHashSet() }
        recent.remove(songId)
        recent.add(songId)
        if (recent.size > recentLimit) {
            recent.remove(recent.first())
        }
    }

    fun topSongs(k: Int): List<Song> {
        return ranking.top(k)
    }

    fun printAnalytics() {
        for (song in topSongs(songs.size)) {
            println("  ${song.title} (${song.uniqueListens} unique listens)")
        }
    }

    fun recentlyPlayed(userId: String): List<Song> {
        val recent = recentByUser[userId] ?: return emptyList()
        return recent.reversed().map { songs.getValue(it) }
    }
}
