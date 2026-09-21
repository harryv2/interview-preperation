package lld.musicplayer

import lld.musicplayer.entity.Player
import lld.musicplayer.entity.Playlist
import lld.musicplayer.entity.RepeatMode
import lld.musicplayer.entity.Song
import lld.musicplayer.strategies.SequentialOrder
import lld.musicplayer.strategies.ShuffleOrder
import kotlin.random.Random

fun main() {
    val playlist = Playlist("p1", "Road trip")
    playlist.add(Song("s1", "Blinding Lights", "The Weeknd", 200))
    playlist.add(Song("s2", "Levitating", "Dua Lipa", 203))
    playlist.add(Song("s3", "Heat Waves", "Glass Animals", 238))

    val player = Player()
    player.addListener { state, song ->
        println("  $state ${song?.title ?: "-"}")
    }

    println("-- sequential")
    player.load(playlist)
    player.play()
    player.next()
    player.pause()
    player.play()
    player.next()
    player.next()

    println("-- repeat all")
    player.repeat = RepeatMode.ALL
    player.play()
    player.next()
    player.next()
    player.next()

    println("-- shuffle (fixed seed)")
    player.repeat = RepeatMode.OFF
    player.setOrder(ShuffleOrder(Random(42)))
    player.play()
    player.next()
    player.next()

    println("-- back to sequential, previous")
    player.setOrder(SequentialOrder())
    player.play()
    player.next()
    player.previous()
}
