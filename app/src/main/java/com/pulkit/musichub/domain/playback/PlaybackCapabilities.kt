package com.pulkit.musichub.domain.playback

data class PlaybackCapabilities(
    val play: Boolean = false,
    val pause: Boolean = false,
    val next: Boolean = false,
    val previous: Boolean = false,
    val seekTo: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val saveToLibrary: Boolean = false
) {
    val canTogglePlayback: Boolean
        get() = play || pause
}
