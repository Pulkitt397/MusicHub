package com.pulkit.musichub.core.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulkit.musichub.core.ui.theme.AccentOnSage
import com.pulkit.musichub.core.ui.theme.AccentSage
import com.pulkit.musichub.core.ui.theme.AppleCardSurface
import com.pulkit.musichub.core.ui.theme.AppleCardSurfaceRaised
import com.pulkit.musichub.core.ui.theme.ProviderAmazonMusic
import com.pulkit.musichub.core.ui.theme.ProviderSpotify
import com.pulkit.musichub.core.ui.theme.ProviderYouTubeMusic
import com.pulkit.musichub.core.ui.theme.StatusSuccess
import com.pulkit.musichub.core.ui.theme.StatusWarning
import com.pulkit.musichub.core.ui.theme.TextMuted
import com.pulkit.musichub.core.ui.theme.TextPrimary
import com.pulkit.musichub.core.ui.theme.TextSecondary
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.providers.ServiceAvailability

@Composable
fun ProviderPicker(
    selectedProvider: ProviderId,
    sessions: List<PlaybackSnapshot>,
    installedProviders: Set<ProviderId>,
    onSelectProvider: (ProviderId) -> Unit,
    modifier: Modifier = Modifier
) {
    val providers = listOf(
        ProviderId.SPOTIFY to "Spotify",
        ProviderId.YOUTUBE_MUSIC to "YouTube",
        ProviderId.AMAZON_MUSIC to "Amazon"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        providers.forEach { (provider, label) ->
            val isSelected = provider == selectedProvider
            val providerSessions = sessions.filter { it.providerId == provider }
            val isPlaying = providerSessions.any { it.isPlaying }
            val isInstalled = installedProviders.contains(provider)
            val status = when {
                isPlaying -> "playing"
                providerSessions.isNotEmpty() -> "paused"
                isInstalled -> "no active session"
                else -> "not installed"
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelectProvider(provider) }
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ProviderDot(
                    providerId = provider,
                    isActive = providerSessions.isNotEmpty(),
                    isInstalled = isInstalled,
                    isPlaying = isPlaying
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProviderDot(
    providerId: ProviderId,
    isActive: Boolean,
    isInstalled: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val brandColor = providerColor(providerId)
    val color = when {
        isActive -> brandColor
        isInstalled -> brandColor.copy(alpha = 0.65f)
        else -> TextMuted.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier.size(if (isPlaying) 9.dp else 7.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.25f))
            )
        }
        Box(
            modifier = Modifier
                .size(if (isPlaying) 5.dp else 7.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun ProviderSourceLabel(
    providerId: ProviderId,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderDot(
            providerId = providerId,
            isActive = true,
            isInstalled = true,
            isPlaying = isPlaying
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = if (isPlaying) {
                "Playing on ${providerId.displayName}"
            } else {
                providerId.displayName
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtworkImage(
    artworkUri: Uri?,
    artworkBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentDescription: String? = "Album artwork"
) {
    var uriFailed by remember(artworkUri) { mutableStateOf(false) }
    val bitmap = artworkBitmap

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(AppleCardSurface),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null && (artworkUri == null || uriFailed) -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            artworkUri != null && !uriFailed -> {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { uriFailed = true }
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No artwork available",
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    onEnableAccessClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppleCardSurface)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StatusWarning.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Allow media access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Needed to see and control active sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Android notification access lets MusicHub read the current media session and its playback controls. " +
                "This access can expose other notifications to a listener, but MusicHub only reads active media sessions and keeps everything on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onEnableAccessClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentSage,
                contentColor = AccentOnSage
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Open Android Settings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ServiceRow(
    service: ServiceAvailability,
    activeSession: PlaybackSnapshot?,
    onOpenClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = providerColor(service.providerId)
    val statusText = when {
        activeSession?.isPlaying == true -> "Playing now"
        activeSession != null -> "Paused"
        service.isInstalled -> "Installed on this phone"
        else -> "Not installed"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (service.isInstalled) accentColor else TextMuted)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = service.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    activeSession?.isPlaying == true -> StatusSuccess
                    service.isInstalled -> TextSecondary
                    else -> TextMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (service.isInstalled) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search in ${service.appName}",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Button(
            onClick = onOpenClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.10f),
                contentColor = TextPrimary
            )
        ) {
            Text(
                text = if (service.isInstalled) "Open" else "Install",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun providerColor(providerId: ProviderId): Color = when (providerId) {
    ProviderId.SPOTIFY -> ProviderSpotify
    ProviderId.YOUTUBE_MUSIC -> ProviderYouTubeMusic
    ProviderId.AMAZON_MUSIC -> ProviderAmazonMusic
    ProviderId.GENERIC -> TextMuted
}
