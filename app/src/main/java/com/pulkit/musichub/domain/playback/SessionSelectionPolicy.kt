package com.pulkit.musichub.domain.playback

/**
 * Selection is explicit: an existing valid selection always wins. Automatic selection only
 * happens when nothing is selected (first launch, or the selected session disappeared).
 */
object SessionSelectionPolicy {

    fun resolveSelection(
        sessions: List<PlaybackSnapshot>,
        currentSelectionKey: String?
    ): String? {
        if (sessions.isEmpty()) return null

        if (currentSelectionKey != null &&
            sessions.any { it.sessionKey == currentSelectionKey }
        ) {
            return currentSelectionKey
        }

        return sessions.firstOrNull { it.isPlaying && it.providerId != ProviderId.GENERIC }?.sessionKey
            ?: sessions.firstOrNull { it.providerId != ProviderId.GENERIC }?.sessionKey
            ?: sessions.first().sessionKey
    }

    fun sessionForProvider(
        sessions: List<PlaybackSnapshot>,
        providerId: ProviderId
    ): PlaybackSnapshot? {
        val matching = sessions.filter { it.providerId == providerId }
        if (matching.isEmpty()) return null
        return matching.firstOrNull { it.isPlaying } ?: matching.first()
    }
}
