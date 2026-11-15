package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.audio.BloomeeAudioEngine
import com.example.auth.signOutUser
import com.example.data.local.PreferencesManager
import com.example.data.model.TrackItem
import com.example.data.repository.BloomeeFirestoreRepository
import com.example.data.repository.MusicCatalogRepository
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.CollabHubScreen
import com.example.ui.screens.CreatePlaylistDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FullPlayerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinByCodeDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.BloomeeTunesTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.BloomeeViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object BloomeeRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val COLLAB = "collab"
    const val LIBRARY = "library"
    const val EQUALIZER = "equalizer"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"

    fun playlistDetail(playlistId: String) = "playlist_detail/$playlistId"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TopTabs = listOf(
    NavTab(BloomeeRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(BloomeeRoutes.EXPLORE, "Explore", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_explore"),
    NavTab(BloomeeRoutes.COLLAB, "Collab", Icons.Rounded.Group, Icons.Outlined.Group, "nav_collab"),
    NavTab(BloomeeRoutes.LIBRARY, "Library", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library")
)

@Composable
fun MainFeatureScreen(
    currentUserId: String,
    isGuest: Boolean = false,
    onRequestSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BloomeeViewModel = viewModel(
        key = "$currentUserId:$isGuest",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val firestoreRepo = if (isGuest) {
                    null
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    BloomeeFirestoreRepository(db, Firebase.auth)
                }
                BloomeeViewModel(
                    firestoreRepo = firestoreRepo,
                    catalogRepo = MusicCatalogRepository(),
                    audioEngine = BloomeeAudioEngine(app),
                    preferencesManager = PreferencesManager(app),
                    currentUserId = currentUserId,
                    isGuest = isGuest
                )
            }
        }
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val homeExplore by viewModel.homeExplore.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val likedTracksState by viewModel.likedTracksState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val playlistsState by viewModel.playlistsState.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracksState by viewModel.selectedPlaylistTracksState.collectAsStateWithLifecycle()
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val likedIds = remember(likedTracksState) {
        ((likedTracksState as? UiState.Success)?.data ?: emptyList()).map { it.trackId }.toSet()
    }
    val playlists = remember(playlistsState) {
        (playlistsState as? UiState.Success)?.data ?: emptyList()
    }

    val dynamicAccent: Color = remember(preferences.dynamicArtworkAccent, playerState.currentTrack?.trackId) {
        if (preferences.dynamicArtworkAccent && playerState.currentTrack != null) {
            parseHexColor(playerState.currentTrack?.accentHex ?: "#FF4D8D", BloomCoral)
        } else {
            BloomCoral
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatus()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var showFullPlayer by remember { mutableStateOf(false) }
    var trackPendingPlaylistAdd by remember { mutableStateOf<TrackItem?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showJoinCodeDialog by remember { mutableStateOf(false) }

    val userLabel = remember(currentUserId, isGuest) {
        if (isGuest) {
            "Guest Listener (Local Session)"
        } else {
            val user = Firebase.auth.currentUser
            user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email
                ?: "Bloomee Listener"
        }
    }

    BloomeeTunesTheme(
        darkTheme = preferences.darkTheme,
        accentColor = dynamicAccent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding()
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        Column(modifier = Modifier.navigationBarsPadding()) {
                            MiniPlayerBar(
                                playerState = playerState,
                                onOpenFullPlayer = { showFullPlayer = true },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSkipNext = viewModel::skipNext
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                windowInsets = WindowInsets(0, 0, 0, 0)
                            ) {
                                TopTabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            TopTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = BloomeeRoutes.HOME
                            ) {
                                composable(BloomeeRoutes.HOME) {
                                    HomeScreen(
                                        homeExplore = homeExplore,
                                        likedTracksState = likedTracksState,
                                        historyState = historyState,
                                        playlistsState = playlistsState,
                                        isGuest = isGuest,
                                        currentTrackId = playerState.currentTrack?.trackId,
                                        onPlayTrack = viewModel::playTrack,
                                        onPlayQueue = { tracks -> viewModel.playQueue(tracks, 0) },
                                        onToggleLike = viewModel::toggleLikeTrack,
                                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                        onAddToQueue = viewModel::addToQueue,
                                        onSmartReplace = { viewModel.smartReplaceTrack(it) },
                                        onOpenPlaylist = { id ->
                                            viewModel.selectPlaylist(id)
                                            navController.navigate(BloomeeRoutes.playlistDetail(id))
                                        },
                                        onNavigateToCollab = {
                                            navController.navigate(BloomeeRoutes.COLLAB) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onOpenEqualizer = {
                                            navController.navigate(BloomeeRoutes.EQUALIZER)
                                        },
                                         onSelectSinger = { singer ->
                                             viewModel.selectIndianSinger(singer)
                                             navController.navigate(BloomeeRoutes.EXPLORE) {
                                                 popUpTo(navController.graph.findStartDestination().id) {
                                                     saveState = true
                                                 }
                                                 launchSingleTop = true
                                                 restoreState = true
                                             }
                                         },
                                         onRequestSignIn = onRequestSignIn,
                                         onRetryCharts = { viewModel.loadCharts(homeExplore.selectedGenre) }
                                     )
                                 }

                                 composable(BloomeeRoutes.EXPLORE) {
                                     ExploreScreen(
                                         homeExplore = homeExplore,
                                         likedIds = likedIds,
                                         currentTrackId = playerState.currentTrack?.trackId,
                                         onSearchQueryChange = viewModel::updateSearchQuery,
                                         onExecuteSearch = { viewModel.executeSearch() },
                                         onSelectGenre = viewModel::loadCharts,
                                         onSelectSinger = viewModel::selectIndianSinger,
                                         onPlayTrack = viewModel::playTrack,
                                         onToggleLike = viewModel::toggleLikeTrack,
                                         onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                         onAddToQueue = viewModel::addToQueue,
                                         onSmartReplace = { viewModel.smartReplaceTrack(it) }
                                     )
                                 }

                                composable(BloomeeRoutes.COLLAB) {
                                    CollabHubScreen(
                                        playlistsState = playlistsState,
                                        isGuest = isGuest,
                                        onOpenPlaylist = { id ->
                                            viewModel.selectPlaylist(id)
                                            navController.navigate(BloomeeRoutes.playlistDetail(id))
                                        },
                                        onShowCreateDialog = { showCreatePlaylistDialog = true },
                                        onShowJoinDialog = { showJoinCodeDialog = true },
                                        onRequestSignIn = onRequestSignIn
                                    )
                                }

                                composable(BloomeeRoutes.LIBRARY) {
                                    LibraryScreen(
                                        likedTracksState = likedTracksState,
                                        historyState = historyState,
                                        localTracks = localTracks,
                                        preferences = preferences,
                                        isGuest = isGuest,
                                        currentTrackId = playerState.currentTrack?.trackId,
                                        userEmailOrName = userLabel,
                                        onPlayTrack = viewModel::playTrack,
                                        onToggleLike = viewModel::toggleLikeTrack,
                                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                        onAddToQueue = viewModel::addToQueue,
                                        onSmartReplace = { viewModel.smartReplaceTrack(it) },
                                        onImportLocalUris = { uris -> viewModel.importLocalAudioUris(context, uris) },
                                        onExportBackup = { uri -> viewModel.exportLibraryBackup(context, uri) },
                                        onImportBackup = { uri -> viewModel.importLibraryBackup(context, uri) },
                                        onToggleDarkTheme = viewModel::setDarkTheme,
                                        onToggleDynamicAccent = viewModel::setDynamicAccent,
                                        onOpenEqualizer = {
                                            navController.navigate(BloomeeRoutes.EQUALIZER)
                                        },
                                        onRequestSignIn = onRequestSignIn,
                                        onSignOut = {
                                            signOutUser(credentialManager, scope)
                                        }
                                    )
                                }

                                composable(BloomeeRoutes.EQUALIZER) {
                                    EqualizerScreen(
                                        playerState = playerState,
                                        onBack = { navController.popBackStack() },
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onToggleEqEnabled = viewModel::toggleEqEnabled,
                                        onApplyEqPreset = viewModel::applyEqPreset,
                                        onUpdateEqBand = viewModel::updateEqBand,
                                        onBassBoostChange = viewModel::setBassBoostStrength,
                                        onVirtualizerChange = viewModel::setVirtualizerStrength,
                                        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
                                        onCrossfadeChange = viewModel::setCrossfadeSeconds,
                                        onResetEffects = viewModel::resetAudioEffects
                                    )
                                }

                                composable(
                                    route = BloomeeRoutes.PLAYLIST_DETAIL,
                                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                                ) { entry ->
                                    val pid = entry.arguments?.getString("playlistId").orEmpty()
                                    LaunchedEffect(pid) {
                                        viewModel.selectPlaylist(pid)
                                    }
                                    PlaylistDetailScreen(
                                        playlist = selectedPlaylist,
                                        tracksState = selectedPlaylistTracksState,
                                        currentUserId = currentUserId,
                                        currentPlayingTrackId = playerState.currentTrack?.trackId,
                                        chartTracks = homeExplore.chartTracks,
                                        searchResults = homeExplore.searchResults,
                                        searchQuery = homeExplore.searchQuery,
                                        isSearching = homeExplore.isSearching,
                                        onBack = {
                                            viewModel.selectPlaylist(null)
                                            navController.popBackStack()
                                        },
                                        onSyncMissingTracks = viewModel::syncSelectedPlaylistTracksIfNeeded,
                                        onPlayTracks = viewModel::playQueue,
                                        onToggleUpvote = { trackDocId ->
                                            viewModel.toggleTrackUpvote(pid, trackDocId)
                                        },
                                        onRemoveTrack = viewModel::removeTrackFromPlaylist,
                                        onSmartReplacePlaylistTrack = { pl, pt ->
                                            viewModel.smartReplaceTrack(
                                                track = pt.toTrackItem(pl.accentColorHex),
                                                playlistId = pl.id,
                                                playlistTrackItemId = pt.id
                                            )
                                        },
                                        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onExecuteSearch = { viewModel.executeSearch() },
                                        onToggleCollaborativeMode = viewModel::setCollaborativeMode,
                                        onRemoveCollaborator = viewModel::removeCollaborator,
                                        onLeavePlaylist = { pl ->
                                            viewModel.leavePlaylist(pl) {
                                                viewModel.selectPlaylist(null)
                                                navController.popBackStack()
                                            }
                                        },
                                        onDeletePlaylist = { pl ->
                                            viewModel.deletePlaylist(pl) {
                                                viewModel.selectPlaylist(null)
                                                navController.popBackStack()
                                            }
                                        },
                                        onCopyInviteCode = { code ->
                                            viewModel.postStatus("Copied invite code $code")
                                        }
                                    )
                                }
                            }
                        }

                        if (useRail && playerState.currentTrack != null) {
                            MiniPlayerBar(
                                playerState = playerState,
                                onOpenFullPlayer = { showFullPlayer = true },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSkipNext = viewModel::skipNext,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .widthIn(max = 560.dp)
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }

            if (showFullPlayer) {
                val activeId = playerState.currentTrack?.trackId
                FullPlayerSheet(
                    playerState = playerState,
                    lyricsState = lyricsState,
                    lyricsOffsetMs = preferences.lyricsOffsetMs,
                    isLiked = activeId != null && activeId in likedIds,
                    onDismiss = { showFullPlayer = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleLike = viewModel::toggleLikeTrack,
                    onAddToPlaylist = { trackPendingPlaylistAdd = it },
                    onSmartReplace = { viewModel.smartReplaceTrack(it) },
                    onPlayQueueItem = { idx -> viewModel.playQueue(playerState.queue, idx) },
                    onAdjustLyricsOffset = viewModel::adjustLyricsOffset,
                    onResetLyricsOffset = viewModel::resetLyricsOffset,
                    onToggleEqEnabled = viewModel::toggleEqEnabled,
                    onApplyEqPreset = viewModel::applyEqPreset,
                    onUpdateEqBand = viewModel::updateEqBand,
                    onBassBoostChange = viewModel::setBassBoostStrength,
                    onVirtualizerChange = viewModel::setVirtualizerStrength,
                    onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
                    onSetCrossfadeSeconds = viewModel::setCrossfadeSeconds,
                    onOpenFullEqualizerScreen = {
                        showFullPlayer = false
                        navController.navigate(BloomeeRoutes.EQUALIZER)
                    },
                    onSeekBy = viewModel::seekBy,
                    onPlaybackPitchChange = viewModel::setPlaybackPitch,
                    onPlayerVolumeChange = viewModel::setPlayerVolume,
                    onToggleMute = viewModel::toggleMute,
                    onToggleAbLoop = viewModel::toggleAbLoopPoint,
                    onClearAbLoop = viewModel::clearAbLoop,
                    onSetSleepTimerMinutes = viewModel::setSleepTimerMinutes,
                    onSetSleepAtTrackEnd = viewModel::setSleepAtEndOfTrack,
                    onCancelSleepTimer = viewModel::cancelSleepTimer,
                    onRemoveQueueItem = viewModel::removeFromQueue,
                    onMoveQueueItem = viewModel::moveQueueItem,
                    onClearUpcomingQueue = viewModel::clearUpcomingQueue,
                    onShuffleUpcomingQueue = viewModel::shuffleUpcomingQueue,
                    onNotifyAction = viewModel::postStatus
                )
            }

            trackPendingPlaylistAdd?.let { pendingTrack ->
                AddToPlaylistSheet(
                    track = pendingTrack,
                    playlists = playlists,
                    isGuest = isGuest,
                    onRequestSignIn = onRequestSignIn,
                    onDismiss = { trackPendingPlaylistAdd = null },
                    onSelectPlaylist = { pl ->
                        viewModel.addTrackToPlaylist(pl, pendingTrack)
                        trackPendingPlaylistAdd = null
                    },
                    onCreateNewPlaylistClick = {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onConfirm = { title, desc, hex, isCollab ->
                        showCreatePlaylistDialog = false
                        viewModel.createPlaylist(title, desc, hex, isCollab) { created ->
                            val pending = trackPendingPlaylistAdd
                            if (pending != null) {
                                viewModel.addTrackToPlaylist(created, pending)
                                trackPendingPlaylistAdd = null
                            } else {
                                viewModel.selectPlaylist(created.id)
                                navController.navigate(BloomeeRoutes.playlistDetail(created.id))
                            }
                        }
                    }
                )
            }

            if (showJoinCodeDialog) {
                JoinByCodeDialog(
                    onDismiss = { showJoinCodeDialog = false },
                    onJoin = { code ->
                        showJoinCodeDialog = false
                        viewModel.joinPlaylistByCode(code) { playlistId ->
                            viewModel.selectPlaylist(playlistId)
                            navController.navigate(BloomeeRoutes.playlistDetail(playlistId))
                        }
                    }
                )
            }
        }
    }
}
