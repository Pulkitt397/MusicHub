package com.pulkit.musichub.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulkit.musichub.core.ui.components.ServiceRow
import com.pulkit.musichub.core.ui.theme.AccentSage
import com.pulkit.musichub.core.ui.theme.AppleCardSurface
import com.pulkit.musichub.core.ui.theme.AppleDarkBackground
import com.pulkit.musichub.core.ui.theme.StatusSuccess
import com.pulkit.musichub.core.ui.theme.StatusWarning
import com.pulkit.musichub.core.ui.theme.TextMuted
import com.pulkit.musichub.core.ui.theme.TextPrimary
import com.pulkit.musichub.core.ui.theme.TextSecondary
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.providers.ServiceAvailability

@Composable
fun SettingsScreen(
    permissionGranted: Boolean,
    listenerConnected: Boolean,
    services: List<ServiceAvailability>,
    sessions: List<PlaybackSnapshot>,
    onManagePermissionClick: () -> Unit,
    onOpenService: (ProviderId) -> Unit,
    onInstallService: (ProviderId) -> Unit,
    onSearchService: (ProviderId) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppleDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeader(
            onBackClick = onBackClick,
            onRefresh = onRefresh
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsTitle()
            }

            item {
                AccessCard(
                    permissionGranted = permissionGranted,
                    listenerConnected = listenerConnected,
                    onManagePermissionClick = onManagePermissionClick
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppleCardSurface)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    SectionHeading(
                        title = "Supported services",
                        subtitle = "Sessions are read only from these apps"
                    )

                    services.forEach { service ->
                        ServiceRow(
                            service = service,
                            activeSession = sessions.firstOrNull { it.providerId == service.providerId },
                            onOpenClick = {
                                if (service.isInstalled) {
                                    onOpenService(service.providerId)
                                } else {
                                    onInstallService(service.providerId)
                                }
                            },
                            onSearchClick = { onSearchService(service.providerId) }
                        )
                    }
                }
            }

            item {
                PrivacyCard()
            }

            item {
                DiagnosticsCard(
                    versionName = versionName,
                    permissionGranted = permissionGranted,
                    listenerConnected = listenerConnected,
                    activeSessionCount = sessions.size,
                    installedServiceCount = services.count { it.isInstalled },
                    onCopy = {
                        val diagnostics = buildDiagnostics(
                            context = context,
                            versionName = versionName,
                            permissionGranted = permissionGranted,
                            listenerConnected = listenerConnected,
                            sessions = sessions,
                            services = services
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("MusicHub diagnostics", diagnostics)
                        )
                        Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Text(
                    text = "MusicHub is an independent media-session controller and app launcher. " +
                        "Spotify, YouTube Music and Amazon Music are trademarks of their respective owners. " +
                        "MusicHub does not provide, host or decode audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "MusicHub",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh sessions",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsTitle() {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Access, services and local diagnostics",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun AccessCard(
    permissionGranted: Boolean,
    listenerConnected: Boolean,
    onManagePermissionClick: () -> Unit
) {
    val statusColor = if (permissionGranted) StatusSuccess else StatusWarning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppleCardSurface)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (permissionGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when {
                        !permissionGranted -> "Required to read active media sessions"
                        listenerConnected -> "Access granted, listener connected"
                        else -> "Access granted, waiting for Android to connect"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (permissionGranted) TextSecondary else StatusWarning
                )
            }
            StatusPill(
                text = if (permissionGranted) "ON" else "OFF",
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Android grants notification access to media-session listeners. MusicHub reads active media sessions and their transport controls only. " +
                "Notification titles and bodies are never stored or sent anywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onManagePermissionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (permissionGranted) Color.White.copy(alpha = 0.12f) else AccentSage,
                contentColor = if (permissionGranted) TextPrimary else Color(0xFF14201A)
            )
        ) {
            Text(
                text = if (permissionGranted) "Manage in Android Settings" else "Enable in Android Settings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppleCardSurface)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentSage.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentSage,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "No accounts, analytics, cloud sync or MusicHub server. Session metadata stays on this phone. " +
                "Album artwork may be fetched from the artwork URI supplied by the source app, which can use the network.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DiagnosticsCard(
    versionName: String,
    permissionGranted: Boolean,
    listenerConnected: Boolean,
    activeSessionCount: Int,
    installedServiceCount: Int,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppleCardSurface)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Actual values from this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy diagnostics",
                    tint = TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DiagnosticRow("MusicHub", versionName)
        DiagnosticRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        DiagnosticRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
        DiagnosticRow("Notification access", if (permissionGranted) "Granted" else "Not granted")
        DiagnosticRow("Session listener", if (listenerConnected) "Connected" else "Not connected")
        DiagnosticRow("Active sessions", activeSessionCount.toString())
        DiagnosticRow("Installed services", "$installedServiceCount of 3")
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun buildDiagnostics(
    context: Context,
    versionName: String,
    permissionGranted: Boolean,
    listenerConnected: Boolean,
    sessions: List<PlaybackSnapshot>,
    services: List<ServiceAvailability>
): String {
    val serviceLines = services.joinToString(separator = "\n") { service ->
        "- ${service.appName} (${service.packageName}): " +
            if (service.isInstalled) "installed" else "not installed"
    }
    val sessionLines = if (sessions.isEmpty()) {
        "- none"
    } else {
        sessions.joinToString(separator = "\n") { session ->
            "- ${session.providerId.displayName}: ${session.displayTitle} / ${session.displayArtist} " +
                "(${if (session.isPlaying) "playing" else "paused"})"
        }
    }

    return buildString {
        appendLine("MusicHub diagnostics")
        appendLine("Version: $versionName")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Package: ${context.packageName}")
        appendLine("Notification access: $permissionGranted")
        appendLine("Session listener connected: $listenerConnected")
        appendLine("Active sessions:")
        appendLine(sessionLines)
        appendLine("Services:")
        appendLine(serviceLines)
    }.trim()
}
