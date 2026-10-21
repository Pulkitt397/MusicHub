package com.pulkit.musichub.domain.playback

import android.graphics.Bitmap
import android.net.Uri

data class PlaybackSnapshot(
    val sessionKey: String,
    val packageName: String,
    val providerId: ProviderId,
    val title: String?,
    val artist: String?,
    val album: String?,
    val artworkUri: Uri? = null,
    val artworkBitmap: Bitmap? = null,
    val durationMs: Long? = null,
    val positionMs: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val capabilities: PlaybackCapabilities = PlaybackCapabilities(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Now playing"

    val displayArtist: String
        get() = artist?.takeIf { it.isNotBlank() } ?: "Metadata unavailable"

    val hasProgress: Boolean
        get() = durationMs != null && durationMs > 0 && positionMs != null

    val hasSeekCapability: Boolean
        get() = capabilities.seekTo && hasProgress
}
