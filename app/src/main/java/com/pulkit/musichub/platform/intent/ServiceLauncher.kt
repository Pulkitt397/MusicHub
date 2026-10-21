package com.pulkit.musichub.platform.intent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.providers.LaunchResult
import com.pulkit.musichub.domain.providers.ServiceAvailability

class ServiceLauncher(private val context: Context) {

    private val packageManager = context.packageManager

    fun getServiceAvailability(providerId: ProviderId): ServiceAvailability {
        val packageName = providerId.primaryPackageName
        val isInstalled = if (packageName.isNotBlank()) {
            isPackageInstalled(packageName)
        } else {
            false
        }
        return ServiceAvailability(
            providerId = providerId,
            isInstalled = isInstalled,
            packageName = packageName,
            appName = providerId.displayName
        )
    }

    fun getAllKnownServices(): List<ServiceAvailability> {
        return listOf(
            getServiceAvailability(ProviderId.SPOTIFY),
            getServiceAvailability(ProviderId.YOUTUBE_MUSIC),
            getServiceAvailability(ProviderId.AMAZON_MUSIC)
        )
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openServiceHome(providerId: ProviderId): LaunchResult {
        val packageName = providerId.primaryPackageName
        if (packageName.isBlank()) {
            return LaunchResult.Error("No app package associated with ${providerId.displayName}")
        }

        if (!isPackageInstalled(packageName)) {
            val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
            return LaunchResult.NotInstalled(packageName, playStoreUrl)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult.Error("Unable to create launch intent for $packageName")

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            LaunchResult.Success
        } catch (e: Exception) {
            LaunchResult.Error("Failed to launch app: ${e.message}")
        }
    }

    fun openServiceSearch(providerId: ProviderId, query: String): LaunchResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return openServiceHome(providerId)

        val packageName = providerId.primaryPackageName
        if (packageName.isBlank()) {
            return LaunchResult.Error("No app package associated with ${providerId.displayName}")
        }

        if (!isPackageInstalled(packageName)) {
            return LaunchResult.NotInstalled(
                packageName,
                "https://play.google.com/store/apps/details?id=$packageName"
            )
        }

        val encoded = Uri.encode(trimmed)
        val uri = when (providerId) {
            ProviderId.SPOTIFY -> Uri.parse("spotify:search:$encoded")
            ProviderId.YOUTUBE_MUSIC -> Uri.parse("https://music.youtube.com/search?q=$encoded")
            ProviderId.AMAZON_MUSIC -> Uri.parse("https://music.amazon.com/search/$encoded")
            ProviderId.GENERIC -> null
        } ?: return openServiceHome(providerId)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(packageManager) != null) {
            try {
                context.startActivity(intent)
                LaunchResult.Success
            } catch (e: Exception) {
                openServiceHome(providerId)
            }
        } else {
            // The provider exposes no supported search route for this query.
            openServiceHome(providerId)
        }
    }

    fun openPlayStore(packageName: String) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (marketIntent.resolveActivity(packageManager) != null) {
                context.startActivity(marketIntent)
            } else {
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            context.startActivity(webIntent)
        }
    }
}
