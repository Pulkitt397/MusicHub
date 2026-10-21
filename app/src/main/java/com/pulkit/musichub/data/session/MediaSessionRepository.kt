package com.pulkit.musichub.data.session

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.pulkit.musichub.domain.playback.CommandResult
import com.pulkit.musichub.domain.playback.PlaybackCommand
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.playback.SessionSelectionPolicy
import com.pulkit.musichub.platform.notification.MusicSessionListenerService
import com.pulkit.musichub.platform.notification.NotificationAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaSessionRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {
    private val appContext = context.applicationContext
    private val mediaSessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _sessions = MutableStateFlow<List<PlaybackSnapshot>>(emptyList())
    val sessions: StateFlow<List<PlaybackSnapshot>> = _sessions.asStateFlow()

    private val _selectedSessionKey = MutableStateFlow<String?>(null)
    val selectedSessionKey: StateFlow<String?> = _selectedSessionKey.asStateFlow()

    private val _permissionGranted = MutableStateFlow(
        NotificationAccessHelper.isNotificationAccessGranted(appContext)
    )
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    val listenerConnected: StateFlow<Boolean> = MusicSessionListenerService.isServiceConnected

    private val activeControllers = LinkedHashMap<String, MediaController>()
    private val registeredCallbacks = mutableMapOf<String, MediaController.Callback>()
    private val positionAnchors = mutableMapOf<String, PositionAnchor>()
    private val artworkCache = mutableMapOf<String, ArtworkCacheEntry>()

    private var tickerJob: Job? = null
    private var isObserving = false
    private var isListenerAttached = false

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        handleControllersChanged(controllers ?: emptyList())
    }

    init {
        MusicSessionListenerService.onServiceConnectedCallback = {
            mainHandler.post {
                if (isObserving) {
                    evaluatePermission()
                    refreshSessions()
                }
            }
        }
        MusicSessionListenerService.onServiceDisconnectedCallback = {
            mainHandler.post {
                if (isObserving) {
                    clearControllers(clearSelection = false)
                }
            }
        }
    }

    fun startObserving() {
        if (isObserving) {
            evaluatePermission()
            refreshSessions()
            return
        }

        isObserving = true
        _hasScanned.value = false
        evaluatePermission()
        refreshSessions()
        startTicker()
    }

    fun stopObserving() {
        if (!isObserving) return
        isObserving = false
        tickerJob?.cancel()
        tickerJob = null
        detachSessionsListener()
        clearControllers(clearSelection = false)
        _hasScanned.value = false
    }

    fun refreshSessions() {
        if (!isObserving) return

        val granted = evaluatePermission()
        if (!granted || mediaSessionManager == null) {
            clearControllers(clearSelection = !granted)
            _hasScanned.value = true
            return
        }

        attachSessionsListener()
        val component = NotificationAccessHelper.getListenerComponentName(appContext)
        try {
            handleControllersChanged(mediaSessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
            Log.w(TAG, "Media session access is not available yet", e)
            if (!NotificationAccessHelper.isNotificationAccessGranted(appContext)) {
                _permissionGranted.value = false
                detachSessionsListener()
                clearControllers(clearSelection = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read active media sessions", e)
        } finally {
            _hasScanned.value = true
        }
    }

    fun selectSession(sessionKey: String): Boolean {
        if (!activeControllers.containsKey(sessionKey)) return false
        _selectedSessionKey.value = sessionKey
        return true
    }

    fun executeCommand(sessionKey: String, command: PlaybackCommand): CommandResult {
        val controller = activeControllers[sessionKey]
            ?: return CommandResult.Error("That session is no longer active")

        val transport = controller.transportControls
        val state = controller.playbackState
        val actions = state?.actions ?: 0L

        return try {
            val result = when (command) {
                PlaybackCommand.Play -> {
                    if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY) ||
                        PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)
                    ) {
                        transport.play()
                        CommandResult.Success
                    } else {
                        CommandResult.UnsupportedAction("Play is not supported by this session")
                    }
                }

                PlaybackCommand.Pause -> {
                    if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PAUSE) ||
                        PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)
                    ) {
                        transport.pause()
                        CommandResult.Success
                    } else {
                        CommandResult.UnsupportedAction("Pause is not supported by this session")
                    }
                }

                PlaybackCommand.TogglePlayPause -> {
                    if (state?.state == PlaybackState.STATE_PLAYING) {
                        if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PAUSE) ||
                            PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)
                        ) {
                            transport.pause()
                            CommandResult.Success
                        } else {
                            CommandResult.UnsupportedAction("Pause is not supported by this session")
                        }
                    } else {
                        if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY) ||
                            PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_PLAY_PAUSE)
                        ) {
                            transport.play()
                            CommandResult.Success
                        } else {
                            CommandResult.UnsupportedAction("Play is not supported by this session")
                        }
                    }
                }

                PlaybackCommand.SkipToNext -> {
                    if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_SKIP_TO_NEXT)) {
                        transport.skipToNext()
                        CommandResult.Success
                    } else {
                        CommandResult.UnsupportedAction("Next track is not supported by this session")
                    }
                }

                PlaybackCommand.SkipToPrevious -> {
                    if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
                        transport.skipToPrevious()
                        CommandResult.Success
                    } else {
                        CommandResult.UnsupportedAction("Previous track is not supported by this session")
                    }
                }

                is PlaybackCommand.SeekTo -> {
                    val duration = controller.metadata
                        ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                        ?.takeIf { it > 0 }
                    if (PlaybackStateMapper.hasAction(actions, PlaybackState.ACTION_SEEK_TO)) {
                        val target = if (duration != null) {
                            command.positionMs.coerceIn(0L, duration)
                        } else {
                            command.positionMs.coerceAtLeast(0L)
                        }
                        transport.seekTo(target)
                        CommandResult.Success
                    } else {
                        CommandResult.UnsupportedAction("Seeking is not supported by this session")
                    }
                }
            }

            // Follow the session's reported state rather than assuming the command worked.
            mainHandler.postDelayed({
                if (isObserving) refreshSessions()
            }, COMMAND_REFRESH_DELAY_MS)

            result
        } catch (e: SecurityException) {
            Log.w(TAG, "Playback command rejected after access change", e)
            evaluatePermission()
            CommandResult.Error("Media access was revoked. Re-enable notification access.")
        } catch (e: Exception) {
            Log.e(TAG, "Playback command failed", e)
            CommandResult.Error("Playback command failed. Try again from the source app.")
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var elapsedSeconds = 0
            while (isActive) {
                delay(TICK_MS)
                if (!isObserving) break
                if (_sessions.value.any { it.isPlaying }) {
                    recomputeSnapshots()
                }
                elapsedSeconds++
                if (elapsedSeconds >= FULL_REFRESH_SECONDS) {
                    elapsedSeconds = 0
                    refreshSessions()
                }
            }
        }
    }

    private fun evaluatePermission(): Boolean {
        val granted = NotificationAccessHelper.isNotificationAccessGranted(appContext)
        val changed = granted != _permissionGranted.value
        _permissionGranted.value = granted

        if (!granted) {
            if (isListenerAttached) detachSessionsListener()
            clearControllers(clearSelection = true)
        } else if (isObserving) {
            attachSessionsListener()
        }

        if (changed) {
            Log.i(TAG, if (granted) "Notification access granted" else "Notification access revoked")
        }
        return granted
    }

    private fun attachSessionsListener() {
        if (isListenerAttached || mediaSessionManager == null) return
        if (!NotificationAccessHelper.isNotificationAccessGranted(appContext)) return

        try {
            val component = NotificationAccessHelper.getListenerComponentName(appContext)
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                component,
                mainHandler
            )
            isListenerAttached = true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification listener is not bound yet", e)
        }
    }

    private fun detachSessionsListener() {
        if (!isListenerAttached) return
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to detach active session listener", e)
        } finally {
            isListenerAttached = false
        }
    }

    private fun handleControllersChanged(controllers: List<MediaController>) {
        if (!isObserving || !_permissionGranted.value) return

        val supportedControllers = LinkedHashMap<String, MediaController>()
        for (controller in controllers) {
            val packageName = controller.packageName ?: continue
            if (!SUPPORTED_PACKAGES.contains(packageName)) continue
            val key = keyFor(controller)
            supportedControllers.putIfAbsent(key, controller)
        }

        val removedKeys = activeControllers.keys - supportedControllers.keys
        for (key in removedKeys) {
            unregisterController(key)
        }

        for ((key, controller) in supportedControllers) {
            if (!activeControllers.containsKey(key)) {
                activeControllers[key] = controller
                registerControllerCallback(key, controller)
            }
        }

        positionAnchors.keys.retainAll(supportedControllers.keys)
        artworkCache.keys.retainAll(supportedControllers.keys)

        recomputeSnapshots()
        resolveSelection()
    }

    private fun registerControllerCallback(key: String, controller: MediaController) {
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                mainHandler.post { recomputeSnapshots() }
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mainHandler.post { recomputeSnapshots() }
            }

            override fun onSessionDestroyed() {
                mainHandler.post {
                    unregisterController(key)
                    refreshSessions()
                }
            }
        }
        registeredCallbacks[key] = callback
        try {
            controller.registerCallback(callback, mainHandler)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register callback for $key", e)
            registeredCallbacks.remove(key)
        }
    }

    private fun unregisterController(key: String) {
        val controller = activeControllers.remove(key)
        val callback = registeredCallbacks.remove(key)
        if (controller != null && callback != null) {
            try {
                controller.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to unregister callback for $key", e)
            }
        }
        positionAnchors.remove(key)
        artworkCache.remove(key)
    }

    private fun clearControllers(clearSelection: Boolean) {
        for ((key, controller) in activeControllers) {
            val callback = registeredCallbacks[key]
            if (callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to clear callback for $key", e)
                }
            }
        }
        activeControllers.clear()
        registeredCallbacks.clear()
        positionAnchors.clear()
        artworkCache.clear()
        _sessions.value = emptyList()
        if (clearSelection) {
            _selectedSessionKey.value = null
        }
    }

    private fun resolveSelection() {
        val resolved = SessionSelectionPolicy.resolveSelection(
            sessions = _sessions.value,
            currentSelectionKey = _selectedSessionKey.value
        )
        if (resolved != _selectedSessionKey.value) {
            _selectedSessionKey.value = resolved
        }
    }

    private fun recomputeSnapshots() {
        if (activeControllers.isEmpty()) {
            _sessions.value = emptyList()
            resolveSelection()
            return
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val snapshots = activeControllers.map { (key, controller) ->
            buildSnapshot(key, controller, nowElapsed)
        }.sortedWith(
            compareBy<PlaybackSnapshot> { it.providerId.ordinal }
                .thenBy { it.sessionKey }
        )
        _sessions.value = snapshots
        resolveSelection()
    }

    private fun buildSnapshot(
        key: String,
        controller: MediaController,
        nowElapsed: Long
    ): PlaybackSnapshot {
        val packageName = controller.packageName.orEmpty()
        val metadata = controller.metadata
        val state = controller.playbackState

        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val isBuffering = state?.state == PlaybackState.STATE_BUFFERING ||
            state?.state == PlaybackState.STATE_CONNECTING

        val durationMs = metadata
            ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0 }

        val rawPosition = state?.position?.takeIf { it >= 0 }
        val positionMs = calculatePosition(key, state, isPlaying, rawPosition, durationMs, nowElapsed)

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)

        val artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

        val artworkBitmap = cachedArtworkBitmap(key, metadata, nowElapsed)
        val actions = state?.actions ?: 0L

        return PlaybackSnapshot(
            sessionKey = key,
            packageName = packageName,
            providerId = ProviderId.fromPackageName(packageName),
            title = title,
            artist = artist,
            album = album,
            artworkUri = artworkUri,
            artworkBitmap = artworkBitmap,
            durationMs = durationMs,
            positionMs = positionMs,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            capabilities = PlaybackStateMapper.toCapabilities(
                actions = actions,
                durationMs = durationMs,
                positionMs = positionMs
            ),
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun calculatePosition(
        key: String,
        state: PlaybackState?,
        isPlaying: Boolean,
        rawPosition: Long?,
        durationMs: Long?,
        nowElapsed: Long
    ): Long? {
        if (state == null || rawPosition == null) {
            if (!isPlaying) positionAnchors.remove(key)
            return rawPosition
        }

        if (!isPlaying) {
            positionAnchors.remove(key)
            return rawPosition
        }

        val baseElapsed = state.lastPositionUpdateTime.takeIf { it > 0 }
            ?: positionAnchors[key]?.elapsedRealtimeMs
        val basePosition = if (state.lastPositionUpdateTime > 0) {
            rawPosition
        } else {
            positionAnchors[key]?.positionMs ?: rawPosition
        }

        val position = if (baseElapsed != null) {
            PlaybackStateMapper.interpolatePosition(
                basePositionMs = basePosition,
                baseElapsedMs = baseElapsed,
                nowElapsedMs = nowElapsed,
                speed = state.playbackSpeed,
                durationMs = durationMs
            )
        } else {
            rawPosition
        }

        positionAnchors[key] = PositionAnchor(position, nowElapsed)
        return position
    }

    private fun cachedArtworkBitmap(
        key: String,
        metadata: MediaMetadata?,
        nowElapsed: Long
    ): Bitmap? {
        if (metadata == null) return null

        val identity = buildString {
            append(metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
            append('|')
            append(metadata.description)
            append('|')
            append(metadata.getLong(MediaMetadata.METADATA_KEY_DURATION))
            append('|')
            append(metadata.getString(MediaMetadata.METADATA_KEY_ART_URI))
        }

        val cached = artworkCache[key]
        if (cached != null && cached.identity == identity &&
            (cached.bitmap != null || nowElapsed - cached.attemptedAtElapsedMs < ARTWORK_RETRY_MS)
        ) {
            return cached.bitmap
        }

        val bitmap = readArtworkBitmap(metadata)
        artworkCache[key] = ArtworkCacheEntry(
            identity = identity,
            bitmap = bitmap,
            attemptedAtElapsedMs = nowElapsed
        )
        return bitmap
    }

    private fun readArtworkBitmap(metadata: MediaMetadata): Bitmap? {
        return try {
            metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read session artwork bitmap", e)
            null
        }
    }

    private fun keyFor(controller: MediaController): String {
        val packageName = controller.packageName ?: "unknown"
        return "$packageName/${controller.sessionToken.hashCode()}"
    }

    private data class PositionAnchor(
        val positionMs: Long,
        val elapsedRealtimeMs: Long
    )

    private data class ArtworkCacheEntry(
        val identity: String,
        val bitmap: Bitmap?,
        val attemptedAtElapsedMs: Long
    )

    companion object {
        private const val TAG = "MediaSessionRepository"
        private const val TICK_MS = 1_000L
        private const val FULL_REFRESH_SECONDS = 5
        private const val COMMAND_REFRESH_DELAY_MS = 300L
        private const val ARTWORK_RETRY_MS = 5_000L

        private val SUPPORTED_PACKAGES = setOf(
            ProviderId.SPOTIFY.primaryPackageName,
            ProviderId.YOUTUBE_MUSIC.primaryPackageName,
            ProviderId.AMAZON_MUSIC.primaryPackageName
        )
    }
}
