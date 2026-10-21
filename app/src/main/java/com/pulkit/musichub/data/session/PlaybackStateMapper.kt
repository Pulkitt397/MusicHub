package com.pulkit.musichub.data.session

import android.media.session.PlaybackState
import com.pulkit.musichub.domain.playback.PlaybackCapabilities

object PlaybackStateMapper {

    fun toCapabilities(
        actions: Long,
        durationMs: Long?,
        positionMs: Long?
    ): PlaybackCapabilities {
        val canPlay = hasAction(actions, PlaybackState.ACTION_PLAY) ||
            hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)
        val canPause = hasAction(actions, PlaybackState.ACTION_PAUSE) ||
            hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)

        return PlaybackCapabilities(
            play = canPlay,
            pause = canPause,
            next = hasAction(actions, PlaybackState.ACTION_SKIP_TO_NEXT),
            previous = hasAction(actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            seekTo = hasAction(actions, PlaybackState.ACTION_SEEK_TO) &&
                durationMs != null && durationMs > 0 && positionMs != null
        )
    }

    /**
     * PlaybackState.position is a snapshot anchored at lastPositionUpdateTime. When a provider
     * leaves that timestamp empty, fall back to the last position observed by MusicHub.
     */
    fun interpolatePosition(
        basePositionMs: Long,
        baseElapsedMs: Long,
        nowElapsedMs: Long,
        speed: Float,
        durationMs: Long?
    ): Long {
        val safeSpeed = if (speed > 0f) speed else 1f
        val elapsed = (nowElapsedMs - baseElapsedMs).coerceAtLeast(0L)
        val computed = basePositionMs + (elapsed * safeSpeed).toLong()
        return if (durationMs != null && durationMs > 0) {
            computed.coerceIn(0L, durationMs)
        } else {
            computed.coerceAtLeast(0L)
        }
    }

    fun hasAction(actions: Long, action: Long): Boolean = (actions and action) != 0L
}
