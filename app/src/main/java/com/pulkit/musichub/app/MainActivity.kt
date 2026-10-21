package com.pulkit.musichub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulkit.musichub.core.ui.theme.AppleDarkBackground
import com.pulkit.musichub.core.ui.theme.MusicHubTheme
import com.pulkit.musichub.feature.player.PlayerScreen
import com.pulkit.musichub.feature.player.PlayerViewModel
import com.pulkit.musichub.feature.search.SearchScreen
import com.pulkit.musichub.feature.settings.SettingsScreen
import com.pulkit.musichub.platform.notification.NotificationAccessHelper

enum class AppDestination {
    PLAYER,
    SEARCH,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppleDarkBackground
                ) {
                    MusicHubRootNavigation(
                        viewModel = viewModel,
                        onOpenNotificationSettings = {
                            startActivity(
                                NotificationAccessHelper.getNotificationAccessSettingsIntent(this)
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

@Composable
fun MusicHubRootNavigation(
    viewModel: PlayerViewModel,
    onOpenNotificationSettings: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(AppDestination.PLAYER) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = currentDestination != AppDestination.PLAYER) {
        currentDestination = AppDestination.PLAYER
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppleDarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                if (targetState == AppDestination.PLAYER) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    (slideInVertically { it / 5 } + fadeIn()) togetherWith
                        (slideOutVertically { it / 5 } + fadeOut())
                }
            },
            label = "MusicHubNavigation",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { destination ->
            when (destination) {
                AppDestination.PLAYER -> {
                    PlayerScreen(
                        state = uiState,
                        onSelectProvider = { providerId -> viewModel.selectProvider(providerId) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                        onOpenProvider = { providerId -> viewModel.openProvider(providerId) },
                        onInstallProvider = { providerId -> viewModel.installProvider(providerId) },
                        onNavigateToSearch = { currentDestination = AppDestination.SEARCH },
                        onNavigateToSettings = { currentDestination = AppDestination.SETTINGS },
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                }

                AppDestination.SEARCH -> {
                    SearchScreen(
                        initialProvider = uiState.selectedProvider,
                        services = uiState.services,
                        onBackClick = { currentDestination = AppDestination.PLAYER },
                        onSearchInProvider = { providerId, query ->
                            viewModel.searchInProvider(providerId, query)
                        },
                        onOpenProvider = { providerId -> viewModel.openProvider(providerId) },
                        onInstallProvider = { providerId -> viewModel.installProvider(providerId) }
                    )
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        permissionGranted = uiState.permissionGranted,
                        listenerConnected = uiState.listenerConnected,
                        services = uiState.services,
                        sessions = uiState.sessions,
                        onManagePermissionClick = onOpenNotificationSettings,
                        onOpenService = { providerId -> viewModel.openProvider(providerId) },
                        onInstallService = { providerId -> viewModel.installProvider(providerId) },
                        onSearchService = { providerId ->
                            viewModel.selectProvider(providerId)
                            currentDestination = AppDestination.SEARCH
                        },
                        onRefresh = { viewModel.refresh() },
                        onBackClick = { currentDestination = AppDestination.PLAYER }
                    )
                }
            }
        }
    }
}
