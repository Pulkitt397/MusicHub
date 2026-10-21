package com.pulkit.musichub.app

import android.app.Application
import com.pulkit.musichub.data.session.MediaSessionRepository
import com.pulkit.musichub.domain.usecase.ExecutePlaybackCommandUseCase
import com.pulkit.musichub.domain.usecase.ObservePlaybackUseCase
import com.pulkit.musichub.domain.usecase.ProviderUseCases
import com.pulkit.musichub.domain.usecase.SelectSessionUseCase
import com.pulkit.musichub.platform.intent.ServiceLauncher

class MusicHubApplication : Application() {

    lateinit var mediaSessionRepository: MediaSessionRepository
        private set

    lateinit var serviceLauncher: ServiceLauncher
        private set

    lateinit var observePlaybackUseCase: ObservePlaybackUseCase
        private set

    lateinit var selectSessionUseCase: SelectSessionUseCase
        private set

    lateinit var executePlaybackCommandUseCase: ExecutePlaybackCommandUseCase
        private set

    lateinit var providerUseCases: ProviderUseCases
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        mediaSessionRepository = MediaSessionRepository(this)
        serviceLauncher = ServiceLauncher(this)

        observePlaybackUseCase = ObservePlaybackUseCase(mediaSessionRepository)
        selectSessionUseCase = SelectSessionUseCase(mediaSessionRepository)
        executePlaybackCommandUseCase = ExecutePlaybackCommandUseCase(mediaSessionRepository)
        providerUseCases = ProviderUseCases(serviceLauncher)
    }

    companion object {
        lateinit var instance: MusicHubApplication
            private set
    }
}
