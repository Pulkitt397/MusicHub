package com.pulkit.musichub.domain.providers

import com.pulkit.musichub.domain.playback.ProviderId

data class ServiceAvailability(
    val providerId: ProviderId,
    val isInstalled: Boolean,
    val packageName: String,
    val appName: String
)

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class NotInstalled(val packageName: String, val playStoreUrl: String) : LaunchResult
    data class Error(val message: String) : LaunchResult
}
