package lld.rippling.musicanalytics.entity

class Song(val id: Int, val title: String) {
    val listeners = HashSet<String>()

    val uniqueListens: Int
        get() = listeners.size
}
