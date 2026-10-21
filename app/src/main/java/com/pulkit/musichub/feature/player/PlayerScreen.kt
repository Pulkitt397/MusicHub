package com.pulkit.musichub.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulkit.musichub.core.ui.components.ArtworkImage
import com.pulkit.musichub.core.ui.components.PermissionCard
import com.pulkit.musichub.core.ui.components.ProviderDot
import com.pulkit.musichub.core.ui.components.ProviderPicker
import com.pulkit.musichub.core.ui.components.ProviderSourceLabel
import com.pulkit.musichub.core.ui.theme.AccentSage
import com.pulkit.musichub.core.ui.theme.AppleCardSurface
import com.pulkit.musichub.core.ui.theme.AppleCardSurfaceRaised
import com.pulkit.musichub.core.ui.theme.AppleDarkBackground
import com.pulkit.musichub.core.ui.theme.TextMuted
import com.pulkit.musichub.core.ui.theme.TextPrimary
import com.pulkit.musichub.core.ui.theme.TextSecondary
import com.pulkit.musichub.domain.playback.PlaybackSnapshot
import com.pulkit.musichub.domain.playback.ProviderId
import java.util.Locale

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onSelectProvider: (ProviderId) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenProvider: (ProviderId) -> Unit,
    onInstallProvider: (ProviderId) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = state.selectedSession
    val provider = state.selectedProvider
    val installedProviders = state.services
        .filter { it.isInstalled }
        .map { it.providerId }
        .toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppleDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        PlayerHeader(
            onNavigateToSettings = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProviderPicker(
            selectedProvider = provider,
            sessions = state.sessions,
            installedProviders = installedProviders,
            onSelectProvider = onSelectProvider
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                !state.permissionGranted -> SetupContent(onManagePermission = onOpenNotificationSettings)
                state.isLoading -> LoadingContent()
                session != null -> SessionContent(
                    session = session,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekTo = onSeekTo
                )

                else -> EmptyContent(
                    provider = provider,
                    isInstalled = state.isProviderInstalled,
                    otherPlayingSession = state.otherPlayingSession,
                    onSwitchToPlaying = onSelectProvider
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HandoffRow(
            provider = provider,
            isInstalled = state.isProviderInstalled,
            onSearchClick = onNavigateToSearch,
            onOpenClick = { onOpenProvider(provider) },
            onInstallClick = { onInstallProvider(provider) }
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun PlayerHeader(
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentSage.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AccentSage,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "MusicHub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Android media session",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }
        }

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SessionContent(
    session: PlaybackSnapshot,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val reservedHeight = if (session.hasProgress) 264.dp else 218.dp
        val artworkSize = (maxHeight - reservedHeight)
            .coerceIn(140.dp, 340.dp)
            .coerceAtMost(maxWidth)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.size(artworkSize),
                contentAlignment = Alignment.Center
            ) {
                ArtworkImage(
                    artworkUri = session.artworkUri,
                    artworkBitmap = session.artworkBitmap,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 18.dp
                )

                if (session.isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                ProviderSourceLabel(
                    providerId = session.providerId,
                    isPlaying = session.isPlaying
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = session.displayTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        lineHeight = 29.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.displayArtist,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val album = session.album
                if (!album.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (session.hasProgress) {
                ProgressSection(
                    session = session,
                    onSeekTo = onSeekTo
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            TransportControls(
                session = session,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProgressSection(
    session: PlaybackSnapshot,
    onSeekTo: (Long) -> Unit
) {
    val duration = session.durationMs ?: return
    val position = (session.positionMs ?: 0L).coerceIn(0L, duration)
    val canSeek = session.hasSeekCapability

    var isDragging by remember(session.sessionKey) { mutableStateOf(false) }
    var sliderValue by remember(session.sessionKey) {
        mutableFloatStateOf(position.toFloat())
    }

    LaunchedEffect(position, isDragging) {
        if (!isDragging) {
            sliderValue = position.toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (canSeek) {
            Slider(
                value = sliderValue.coerceIn(0f, duration.toFloat()),
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeekTo(sliderValue.toLong())
                },
                valueRange = 0f..duration.toFloat(),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shownPosition = if (canSeek && isDragging) {
                sliderValue.toLong()
            } else {
                position
            }
            Text(
                text = formatDuration(shownPosition),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = "-${formatDuration((duration - shownPosition).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun TransportControls(
    session: PlaybackSnapshot,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    val capabilities = session.capabilities

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (capabilities.previous) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous track",
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }

        val canToggle = capabilities.canTogglePlayback
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(
                    if (canToggle) Color.White else AppleCardSurfaceRaised
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onTogglePlayPause,
                enabled = canToggle,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = when {
                        !canToggle -> "Playback controls unavailable"
                        session.isPlaying -> "Pause"
                        else -> "Play"
                    },
                    tint = if (canToggle) Color.Black else TextMuted,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        if (capabilities.next) {
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next track",
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun SetupContent(
    onManagePermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PermissionCard(onEnableAccessClick = onManagePermission)
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AccentSage,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Looking for active sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun EmptyContent(
    provider: ProviderId,
    isInstalled: Boolean,
    otherPlayingSession: PlaybackSnapshot?,
    onSwitchToPlaying: (ProviderId) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(AppleCardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isInstalled) {
                "${provider.displayName} isn't playing"
            } else {
                "${provider.displayName} isn't installed"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isInstalled) {
                "Start a song in ${provider.displayName}, or open the app to browse your library. MusicHub only shows the session Android exposes."
            } else {
                "Install the app to control its playback through Android media sessions, or choose another service above."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        if (otherPlayingSession != null) {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppleCardSurface)
                    .clickable { onSwitchToPlaying(otherPlayingSession.providerId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProviderDot(
                    providerId = otherPlayingSession.providerId,
                    isActive = true,
                    isInstalled = true,
                    isPlaying = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${otherPlayingSession.providerId.displayName} is playing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = otherPlayingSession.displayTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Switch to ${otherPlayingSession.providerId.displayName}",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HandoffRow(
    provider: ProviderId,
    isInstalled: Boolean,
    onSearchClick: () -> Unit,
    onOpenClick: () -> Unit,
    onInstallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onSearchClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleCardSurface,
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = if (isInstalled) onOpenClick else onInstallClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleCardSurface,
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isInstalled) "Open ${provider.displayName}" else "Install ${provider.displayName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
