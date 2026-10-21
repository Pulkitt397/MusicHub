package com.pulkit.musichub.feature.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulkit.musichub.app.MusicHubApplication
import com.pulkit.musichub.domain.playback.CommandResult
import com.pulkit.musichub.domain.playback.PlaybackCommand
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.playback.SessionSelectionPolicy
import com.pulkit.musichub.domain.providers.LaunchResult
import com.pulkit.musichub.domain.providers.ServiceAvailability
import com.pulkit.musichub.domain.usecase.ExecutePlaybackCommandUseCase
import com.pulkit.musichub.domain.usecase.ProviderUseCases
import com.pulkit.musichub.domain.usecase.SelectSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val selectedProvider: ProviderId = ProviderId.SPOTIFY,
    val selectedSession: PlaybackSnapshot? = null,
    val sessions: List<PlaybackSnapshot> = emptyList(),
    val permissionGranted: Boolean = false,
    val hasCompletedScan: Boolean = false,
    val listenerConnected: Boolean = false,
    val services: List<ServiceAvailability> = emptyList(),
    val otherPlayingSession: PlaybackSnapshot? = null,
    val message: String? = null
) {
    val providerService: ServiceAvailability?
        get() = services.firstOrNull { it.providerId == selectedProvider }

    val isProviderInstalled: Boolean
        get() = providerService?.isInstalled == true

    val isLoading: Boolean
        get() = !hasCompletedScan

    val isPlaying: Boolean
        get() = selectedSession?.isPlaying == true

    val canTogglePlayback: Boolean
        get() = selectedSession?.capabilities?.canTogglePlayback == true
}

class PlayerViewModel(
    private val selectSessionUseCase: SelectSessionUseCase = MusicHubApplication.instance.selectSessionUseCase,
    private val executePlaybackCommandUseCase: ExecutePlaybackCommandUseCase = MusicHubApplication.instance.executePlaybackCommandUseCase,
    private val providerUseCases: ProviderUseCases = MusicHubApplication.instance.providerUseCases,
    private val mediaSessionRepository: com.pulkit.musichub.data.session.MediaSessionRepository = MusicHubApplication.instance.mediaSessionRepository
) : ViewModel() {

    private val _focusedProvider = MutableStateFlow<ProviderId?>(null)
    private val _services = MutableStateFlow(providerUseCases.getServices())
    private val _message = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            combine(
                mediaSessionRepository.sessions,
                _focusedProvider
            ) { sessions, focused ->
                sessions to focused
            }.collect { (sessions, focused) ->
                if (focused != null) {
                    val current = sessions.firstOrNull {
                        it.sessionKey == mediaSessionRepository.selectedSessionKey.value
                    }
                    if (current?.providerId != focused) {
                        SessionSelectionPolicy.sessionForProvider(sessions, focused)?.let {
                            selectSessionUseCase(it.sessionKey)
                        }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        mediaSessionRepository.sessions,
        mediaSessionRepository.selectedSessionKey,
        _focusedProvider,
        mediaSessionRepository.permissionGranted,
        mediaSessionRepository.hasScanned,
        mediaSessionRepository.listenerConnected,
        _services,
        _message
    ) { values ->
        val sessions = values[0] as List<PlaybackSnapshot>
        val selectedKey = values[1] as String?
        val focusedProvider = values[2] as ProviderId?
        val permissionGranted = values[3] as Boolean
        val hasScanned = values[4] as Boolean
        val listenerConnected = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val services = values[6] as List<ServiceAvailability>
        val message = values[7] as String?

        val autoSelected = if (selectedKey != null) {
            sessions.firstOrNull { it.sessionKey == selectedKey }
        } else {
            null
        }

        val fallbackProvider = services
            .firstOrNull { it.isInstalled && it.providerId != ProviderId.GENERIC }
            ?.providerId
            ?: ProviderId.SPOTIFY

        val providerSession = if (focusedProvider != null) {
            SessionSelectionPolicy.sessionForProvider(sessions, focusedProvider)
        } else {
            autoSelected ?: SessionSelectionPolicy.sessionForProvider(
                sessions,
                fallbackProvider
            )
        }

        val effectiveProvider = focusedProvider
            ?: providerSession?.providerId
            ?: autoSelected?.providerId
            ?: fallbackProvider

        val otherPlaying = sessions.firstOrNull {
            it.isPlaying && it.sessionKey != providerSession?.sessionKey
        }

        PlayerUiState(
            selectedProvider = effectiveProvider,
            selectedSession = providerSession,
            sessions = sessions,
            permissionGranted = permissionGranted,
            hasCompletedScan = hasScanned,
            listenerConnected = listenerConnected,
            services = services,
            otherPlayingSession = otherPlaying,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(
            services = _services.value,
            permissionGranted = mediaSessionRepository.permissionGranted.value
        )
    )

    fun onStart() {
        refreshServices()
        mediaSessionRepository.startObserving()
    }

    fun onStop() {
        mediaSessionRepository.stopObserving()
    }

    fun refresh() {
        refreshServices()
        mediaSessionRepository.refreshSessions()
    }

    fun selectProvider(providerId: ProviderId) {
        _focusedProvider.value = providerId
        SessionSelectionPolicy.sessionForProvider(
            mediaSessionRepository.sessions.value,
            providerId
        )?.let { selectSessionUseCase(it.sessionKey) }
    }

    fun togglePlayPause() {
        dispatchCommand(PlaybackCommand.TogglePlayPause)
    }

    fun skipToNext() {
        dispatchCommand(PlaybackCommand.SkipToNext)
    }

    fun skipToPrevious() {
        dispatchCommand(PlaybackCommand.SkipToPrevious)
    }

    fun seekTo(positionMs: Long) {
        dispatchCommand(PlaybackCommand.SeekTo(positionMs))
    }

    fun openProvider(providerId: ProviderId = uiState.value.selectedProvider) {
        when (val result = providerUseCases.openService(providerId)) {
            LaunchResult.Success -> Unit
            is LaunchResult.NotInstalled -> {
                providerUseCases.openPlayStore(result.packageName)
            }

            is LaunchResult.Error -> {
                postMessage(result.message)
            }
        }
    }

    fun installProvider(providerId: ProviderId = uiState.value.selectedProvider) {
        providerUseCases.openPlayStore(providerId.primaryPackageName)
    }

    fun searchInProvider(
        providerId: ProviderId = uiState.value.selectedProvider,
        query: String
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            openProvider(providerId)
            return
        }

        when (val result = providerUseCases.openSearch(providerId, trimmed)) {
            LaunchResult.Success -> Unit
            is LaunchResult.NotInstalled -> {
                providerUseCases.openPlayStore(result.packageName)
            }

            is LaunchResult.Error -> {
                postMessage("Couldn't open ${providerId.displayName} search")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun dispatchCommand(command: PlaybackCommand) {
        val session = uiState.value.selectedSession ?: return
        val result = executePlaybackCommandUseCase(session.sessionKey, command)
        if (result is CommandResult.Error) {
            postMessage(result.message)
        } else if (result is CommandResult.UnsupportedAction) {
            postMessage(result.action)
        }
    }

    private fun refreshServices() {
        _services.value = providerUseCases.getServices()
    }

    private fun postMessage(message: String) {
        _message.value = message
    }

    class Factory(
        @Suppress("UNUSED_PARAMETER") context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel() as T
        }
    }
}
