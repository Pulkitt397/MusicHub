package com.pulkit.musichub

import com.pulkit.musichub.domain.playback.PlaybackCapabilities
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSnapshotTest {

    @Test
    fun `providerId resolves only supported packages`() {
        assertEquals(ProviderId.SPOTIFY, ProviderId.fromPackageName("com.spotify.music"))
        assertEquals(
            ProviderId.YOUTUBE_MUSIC,
            ProviderId.fromPackageName("com.google.android.apps.youtube.music")
        )
        assertEquals(ProviderId.AMAZON_MUSIC, ProviderId.fromPackageName("com.amazon.mp3"))
        assertEquals(ProviderId.GENERIC, ProviderId.fromPackageName("com.google.android.youtube"))
        assertEquals(ProviderId.GENERIC, ProviderId.fromPackageName("com.random.player"))
    }

    @Test
    fun `capabilities calculation behaves correctly`() {
        val capsNoSeek = PlaybackCapabilities(
            play = true,
            pause = true,
            next = true,
            previous = false,
            seekTo = false
        )
        assertTrue(capsNoSeek.canTogglePlayback)
        assertTrue(capsNoSeek.next)
        assertFalse(capsNoSeek.previous)

        val snapshot = PlaybackSnapshot(
            sessionKey = "test_key",
            packageName = "com.spotify.music",
            providerId = ProviderId.SPOTIFY,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180000L,
            positionMs = 30000L,
            isPlaying = true,
            capabilities = capsNoSeek
        )

        assertFalse(snapshot.hasSeekCapability)
        assertEquals("Test Song", snapshot.displayTitle)
        assertEquals("Test Artist", snapshot.displayArtist)
        assertTrue(snapshot.hasProgress)
    }

    @Test
    fun `seek capability requires both seek action and valid duration and position`() {
        val capsWithSeek = PlaybackCapabilities(
            play = true,
            pause = true,
            seekTo = true
        )
        val validSeekSnapshot = PlaybackSnapshot(
            sessionKey = "test_key",
            packageName = "com.spotify.music",
            providerId = ProviderId.SPOTIFY,
            title = null,
            artist = null,
            album = null,
            durationMs = 200000L,
            positionMs = 10000L,
            capabilities = capsWithSeek
        )
        assertTrue(validSeekSnapshot.hasSeekCapability)
        assertEquals("Now playing", validSeekSnapshot.displayTitle)
        assertEquals("Metadata unavailable", validSeekSnapshot.displayArtist)

        assertFalse(validSeekSnapshot.copy(durationMs = null).hasSeekCapability)
        assertFalse(validSeekSnapshot.copy(positionMs = null).hasSeekCapability)
        assertFalse(validSeekSnapshot.copy(durationMs = 0L).hasSeekCapability)
    }

    @Test
    fun `blank metadata uses honest fallbacks`() {
        val snapshot = PlaybackSnapshot(
            sessionKey = "key",
            packageName = "com.amazon.mp3",
            providerId = ProviderId.AMAZON_MUSIC,
            title = "   ",
            artist = "",
            album = null
        )
        assertEquals("Now playing", snapshot.displayTitle)
        assertEquals("Metadata unavailable", snapshot.displayArtist)
        assertFalse(snapshot.hasProgress)
    }

    @Test
    fun `session selection preserves an existing explicit choice`() {
        val playing = session("amazon", ProviderId.AMAZON_MUSIC, isPlaying = true)
        val paused = session("spotify", ProviderId.SPOTIFY, isPlaying = false)

        assertEquals(
            "spotify",
            com.pulkit.musichub.domain.playback.SessionSelectionPolicy.resolveSelection(
                listOf(playing, paused),
                "spotify"
            )
        )
    }

    @Test
    fun `session selection picks a playing session only when nothing is selected`() {
        val paused = session("spotify", ProviderId.SPOTIFY, isPlaying = false)
        val playing = session("amazon", ProviderId.AMAZON_MUSIC, isPlaying = true)

        assertEquals(
            "amazon",
            com.pulkit.musichub.domain.playback.SessionSelectionPolicy.resolveSelection(
                listOf(paused, playing),
                null
            )
        )
        assertNull(
            com.pulkit.musichub.domain.playback.SessionSelectionPolicy.resolveSelection(
                emptyList(),
                "spotify"
            )
        )
    }

    @Test
    fun `session selection replaces a stale selection`() {
        val spotify = session("spotify", ProviderId.SPOTIFY, isPlaying = false)
        assertEquals(
            "spotify",
            com.pulkit.musichub.domain.playback.SessionSelectionPolicy.resolveSelection(
                listOf(spotify),
                "gone"
            )
        )
    }

    @Test
    fun `provider lookup prefers the playing session`() {
        val paused = session("spotify-1", ProviderId.SPOTIFY, isPlaying = false)
        val playing = session("spotify-2", ProviderId.SPOTIFY, isPlaying = true)

        val result = com.pulkit.musichub.domain.playback.SessionSelectionPolicy.sessionForProvider(
            listOf(paused, playing),
            ProviderId.SPOTIFY
        )
        assertEquals("spotify-2", result?.sessionKey)
    }

    private fun session(
        key: String,
        provider: ProviderId,
        isPlaying: Boolean
    ): PlaybackSnapshot = PlaybackSnapshot(
        sessionKey = key,
        packageName = provider.primaryPackageName,
        providerId = provider,
        title = "Title",
        artist = "Artist",
        album = null,
        isPlaying = isPlaying
    )
}
