package com.pulkit.musichub

import android.media.session.PlaybackState
import com.pulkit.musichub.data.session.PlaybackStateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateMapperTest {

    @Test
    fun `play pause action exposes toggle control`() {
        val capabilities = PlaybackStateMapper.toCapabilities(
            actions = PlaybackState.ACTION_PLAY_PAUSE,
            durationMs = 180_000L,
            positionMs = 10_000L
        )

        assertTrue(capabilities.play)
        assertTrue(capabilities.pause)
        assertTrue(capabilities.canTogglePlayback)
        assertFalse(capabilities.next)
        assertFalse(capabilities.previous)
        assertFalse(capabilities.seekTo)
    }

    @Test
    fun `transport actions map to supported capabilities`() {
        val capabilities = PlaybackStateMapper.toCapabilities(
            actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO,
            durationMs = 240_000L,
            positionMs = 30_000L
        )

        assertTrue(capabilities.play)
        assertFalse(capabilities.pause)
        assertTrue(capabilities.next)
        assertTrue(capabilities.previous)
        assertTrue(capabilities.seekTo)
    }

    @Test
    fun `seek requires a usable duration and position`() {
        val withoutDuration = PlaybackStateMapper.toCapabilities(
            actions = PlaybackState.ACTION_SEEK_TO,
            durationMs = null,
            positionMs = 0L
        )
        val withoutPosition = PlaybackStateMapper.toCapabilities(
            actions = PlaybackState.ACTION_SEEK_TO,
            durationMs = 100_000L,
            positionMs = null
        )

        assertFalse(withoutDuration.seekTo)
        assertFalse(withoutPosition.seekTo)
    }

    @Test
    fun `interpolated position advances with elapsed time`() {
        val position = PlaybackStateMapper.interpolatePosition(
            basePositionMs = 10_000L,
            baseElapsedMs = 100_000L,
            nowElapsedMs = 110_000L,
            speed = 1f,
            durationMs = 180_000L
        )

        assertEquals(20_000L, position)
    }

    @Test
    fun `interpolated position respects speed and duration bounds`() {
        val fastPosition = PlaybackStateMapper.interpolatePosition(
            basePositionMs = 10_000L,
            baseElapsedMs = 100_000L,
            nowElapsedMs = 110_000L,
            speed = 2f,
            durationMs = 180_000L
        )
        val clampedPosition = PlaybackStateMapper.interpolatePosition(
            basePositionMs = 20_000L,
            baseElapsedMs = 100_000L,
            nowElapsedMs = 110_000L,
            speed = 1f,
            durationMs = 25_000L
        )

        assertEquals(30_000L, fastPosition)
        assertEquals(25_000L, clampedPosition)
    }

    @Test
    fun `interpolated position never goes negative`() {
        val position = PlaybackStateMapper.interpolatePosition(
            basePositionMs = 5_000L,
            baseElapsedMs = 200_000L,
            nowElapsedMs = 100_000L,
            speed = 1f,
            durationMs = null
        )

        assertEquals(5_000L, position)
    }

    @Test
    fun `zero speed uses normal interpolation`() {
        val position = PlaybackStateMapper.interpolatePosition(
            basePositionMs = 0L,
            baseElapsedMs = 0L,
            nowElapsedMs = 2_000L,
            speed = 0f,
            durationMs = 10_000L
        )

        assertEquals(2_000L, position)
    }
}
