package com.pulkit.musichub.domain.playback

sealed interface PlaybackCommand {
    data object Play : PlaybackCommand
    data object Pause : PlaybackCommand
    data object TogglePlayPause : PlaybackCommand
    data object SkipToNext : PlaybackCommand
    data object SkipToPrevious : PlaybackCommand
    data class SeekTo(val positionMs: Long) : PlaybackCommand
}

sealed interface CommandResult {
    data object Success : CommandResult
    data class Error(val message: String) : CommandResult
    data class UnsupportedAction(val action: String) : CommandResult
}
