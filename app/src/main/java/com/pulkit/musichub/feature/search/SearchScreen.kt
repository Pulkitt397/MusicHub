package com.pulkit.musichub.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulkit.musichub.core.ui.components.ProviderDot
import com.pulkit.musichub.core.ui.theme.AppleCardSurface
import com.pulkit.musichub.core.ui.theme.AppleCardSurfaceRaised
import com.pulkit.musichub.core.ui.theme.AppleDarkBackground
import com.pulkit.musichub.core.ui.theme.TextMuted
import com.pulkit.musichub.core.ui.theme.TextPrimary
import com.pulkit.musichub.core.ui.theme.TextSecondary
import com.pulkit.musichub.domain.playback.ProviderId
import com.pulkit.musichub.domain.providers.ServiceAvailability

@Composable
fun SearchScreen(
    initialProvider: ProviderId,
    services: List<ServiceAvailability>,
    onBackClick: () -> Unit,
    onSearchInProvider: (ProviderId, String) -> Unit,
    onOpenProvider: (ProviderId) -> Unit,
    onInstallProvider: (ProviderId) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val selectedService = services.firstOrNull { it.providerId == selectedProvider }
    val isInstalled = selectedService?.isInstalled == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppleDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        SearchHeader(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose a service, then search in its native app. MusicHub does not mirror catalogs or play audio itself.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            ServiceSelector(
                services = services,
                selectedProvider = selectedProvider,
                onSelect = { selectedProvider = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = {
                    Text(
                        text = "Song, artist, or album",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (isInstalled && query.isNotBlank()) {
                        focusManager.clearFocus()
                        onSearchInProvider(selectedProvider, query)
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppleCardSurface,
                    unfocusedContainerColor = AppleCardSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppleCardSurface)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderDot(
                        providerId = selectedProvider,
                        isActive = false,
                        isInstalled = isInstalled,
                        isPlaying = false
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = selectedProvider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isInstalled) "Installed" else "Not installed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInstalled) TextMuted else TextMuted,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isInstalled) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSearchInProvider(selectedProvider, query)
                        },
                        enabled = query.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = AppleCardSurfaceRaised,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search in ${selectedProvider.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onOpenProvider(selectedProvider) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleCardSurfaceRaised,
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
                            text = "Open ${selectedProvider.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Install ${selectedProvider.displayName} to search and control it through Android media sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onInstallProvider(selectedProvider) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Install ${selectedProvider.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Search opens the service directly. Results, library and playback remain inside the source app.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SearchHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
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
    }
}

@Composable
private fun ServiceSelector(
    services: List<ServiceAvailability>,
    selectedProvider: ProviderId,
    onSelect: (ProviderId) -> Unit
) {
    val providers = listOf(
        ProviderId.SPOTIFY to "Spotify",
        ProviderId.YOUTUBE_MUSIC to "YouTube Music",
        ProviderId.AMAZON_MUSIC to "Amazon Music"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        providers.forEach { (provider, label) ->
            val isInstalled = services.firstOrNull { it.providerId == provider }?.isInstalled == true
            val isSelected = provider == selectedProvider

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.14f)
                        else AppleCardSurface
                    )
                    .clickable { onSelect(provider) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ProviderDot(
                        providerId = provider,
                        isActive = isSelected,
                        isInstalled = isInstalled,
                        isPlaying = false
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (provider) {
                            ProviderId.YOUTUBE_MUSIC -> "YouTube"
                            ProviderId.AMAZON_MUSIC -> "Amazon"
                            else -> label
                        },
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
}
