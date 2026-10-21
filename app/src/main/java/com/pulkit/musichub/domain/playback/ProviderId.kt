package com.pulkit.musichub.domain.playback

enum class ProviderId(val displayName: String, val primaryPackageName: String) {
    SPOTIFY("Spotify", "com.spotify.music"),
    YOUTUBE_MUSIC("YouTube Music", "com.google.android.apps.youtube.music"),
    AMAZON_MUSIC("Amazon Music", "com.amazon.mp3"),
    GENERIC("Media Session", "");

    companion object {
        fun fromPackageName(packageName: String): ProviderId {
            return when {
                packageName.equals("com.spotify.music", ignoreCase = true) -> SPOTIFY
                packageName.equals("com.google.android.apps.youtube.music", ignoreCase = true) -> YOUTUBE_MUSIC
                packageName.equals("com.amazon.mp3", ignoreCase = true) -> AMAZON_MUSIC
                else -> GENERIC
            }
        }
    }
}
