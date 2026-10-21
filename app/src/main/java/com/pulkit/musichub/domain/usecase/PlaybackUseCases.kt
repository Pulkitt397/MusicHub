package com.pulkit.musichub.domain.usecase

import com.pulkit.musichub.data.session.MediaSessionRepository
import com.pulkit.musichub.domain.playback.CommandResult
import com.pulkit.musichub.domain.playback.PlaybackCommand
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.providers.LaunchResult
import com.pulkit.musichub.domain.providers.ServiceAvailability
import com.pulkit.musichub.platform.intent.ServiceLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePlaybackUseCase(private val repository: MediaSessionRepository) {
    fun getSessions(): Flow<List<PlaybackSnapshot>> = repository.sessions

    fun getSelectedSnapshot(): Flow<PlaybackSnapshot?> {
        return combine(repository.sessions, repository.selectedSessionKey) { sessions, selectedKey ->
            if (selectedKey == null) null
            else sessions.firstOrNull { it.sessionKey == selectedKey }
        }
    }
}

class SelectSessionUseCase(private val repository: MediaSessionRepository) {
    operator fun invoke(sessionKey: String) {
        repository.selectSession(sessionKey)
    }
}

class ExecutePlaybackCommandUseCase(private val repository: MediaSessionRepository) {
    operator fun invoke(sessionKey: String, command: PlaybackCommand): CommandResult {
        return repository.executeCommand(sessionKey, command)
    }
}

class ProviderUseCases(private val serviceLauncher: ServiceLauncher) {
    fun getServices(): List<ServiceAvailability> = serviceLauncher.getAllKnownServices()

    fun openService(providerId: ProviderId): LaunchResult = serviceLauncher.openServiceHome(providerId)

    fun openSearch(providerId: ProviderId, query: String): LaunchResult = serviceLauncher.openServiceSearch(providerId, query)

    fun openPlayStore(packageName: String) = serviceLauncher.openPlayStore(packageName)
}
