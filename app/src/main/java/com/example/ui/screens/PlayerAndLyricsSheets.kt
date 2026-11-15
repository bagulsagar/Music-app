package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.EqPreset
import com.example.audio.PlayerUiState
import com.example.audio.RepeatMode
import com.example.data.model.TrackItem
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SonicMint
import com.example.ui.viewmodel.LyricsUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerUiState,
    lyricsState: LyricsUiState,
    lyricsOffsetMs: Long,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: (TrackItem, Boolean) -> Unit,
    onAddToPlaylist: (TrackItem) -> Unit,
    onSmartReplace: (TrackItem) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onUpdateEqBand: (Short, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onOpenFullEqualizerScreen: () -> Unit,
    onSeekBy: (Long) -> Unit = { delta -> onSeekTo((playerState.positionMs + delta).coerceAtLeast(0L)) },
    onPlaybackPitchChange: (Float) -> Unit = {},
    onPlayerVolumeChange: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleAbLoop: () -> Unit = {},
    onClearAbLoop: () -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onSetSleepAtTrackEnd: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onRemoveQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onClearUpcomingQueue: () -> Unit = {},
    onShuffleUpcomingQueue: () -> Unit = {},
    onNotifyAction: (String) -> Unit = {}
) {
    val track = playerState.currentTrack ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember { mutableIntStateOf(0) } // 0=Player, 1=Lyrics, 2=Equalizer, 3=Queue

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = ObsidianCanvas
    ) {
        FullPlayerContent(
            track = track,
            playerState = playerState,
            lyricsState = lyricsState,
            lyricsOffsetMs = lyricsOffsetMs,
            isLiked = isLiked,
            activeTab = activeTab,
            onTabChange = { activeTab = it },
            onDismiss = onDismiss,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onToggleLike = onToggleLike,
            onAddToPlaylist = onAddToPlaylist,
            onSmartReplace = onSmartReplace,
            onPlayQueueItem = onPlayQueueItem,
            onAdjustLyricsOffset = onAdjustLyricsOffset,
            onResetLyricsOffset = onResetLyricsOffset,
            onToggleEqEnabled = onToggleEqEnabled,
            onApplyEqPreset = onApplyEqPreset,
            onUpdateEqBand = onUpdateEqBand,
            onBassBoostChange = onBassBoostChange,
            onVirtualizerChange = onVirtualizerChange,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onSetCrossfadeSeconds = onSetCrossfadeSeconds,
            onOpenFullEqualizerScreen = onOpenFullEqualizerScreen,
            onSeekBy = onSeekBy,
            onPlaybackPitchChange = onPlaybackPitchChange,
            onPlayerVolumeChange = onPlayerVolumeChange,
            onToggleMute = onToggleMute,
            onToggleAbLoop = onToggleAbLoop,
            onClearAbLoop = onClearAbLoop,
            onSetSleepTimerMinutes = onSetSleepTimerMinutes,
            onSetSleepAtTrackEnd = onSetSleepAtTrackEnd,
            onCancelSleepTimer = onCancelSleepTimer,
            onRemoveQueueItem = onRemoveQueueItem,
            onMoveQueueItem = onMoveQueueItem,
            onClearUpcomingQueue = onClearUpcomingQueue,
            onShuffleUpcomingQueue = onShuffleUpcomingQueue,
            onNotifyAction = onNotifyAction,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullPlayerContent(
    track: TrackItem,
    playerState: PlayerUiState,
    lyricsState: LyricsUiState,
    lyricsOffsetMs: Long,
    isLiked: Boolean,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: (TrackItem, Boolean) -> Unit,
    onAddToPlaylist: (TrackItem) -> Unit,
    onSmartReplace: (TrackItem) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onUpdateEqBand: (Short, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onOpenFullEqualizerScreen: () -> Unit,
    onSeekBy: (Long) -> Unit = {},
    onPlaybackPitchChange: (Float) -> Unit = {},
    onPlayerVolumeChange: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleAbLoop: () -> Unit = {},
    onClearAbLoop: () -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onSetSleepAtTrackEnd: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onRemoveQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onClearUpcomingQueue: () -> Unit = {},
    onShuffleUpcomingQueue: () -> Unit = {},
    onNotifyAction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showSpeedPitchDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var largeLyricsText by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ObsidianCanvas,
                        ObsidianCanvas
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_minimize_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize player",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { PlayerModePill("Player", activeTab == 0, null) { onTabChange(0) } }
                item { PlayerModePill("Lyrics", activeTab == 1, Icons.Rounded.Lyrics) { onTabChange(1) } }
                item { PlayerModePill("EQ", activeTab == 2, Icons.Rounded.Equalizer) { onTabChange(2) } }
                item { PlayerModePill("Queue", activeTab == 3, Icons.AutoMirrored.Rounded.QueueMusic) { onTabChange(3) } }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Artwork stage + Live telemetry chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 248.dp, maxHeight = 248.dp)
                                .fillMaxWidth(0.72f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtImage(
                                artworkUrl = track.artworkUrl,
                                contentDescription = "${track.title} artwork",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                cornerRadius = 28.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live DSP & Tools Status Badges
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            if (playerState.eqEnabled) {
                                TelemetryBadge(
                                    text = "EQ • ${playerState.eqPreset.label}",
                                    color = SonicMint,
                                    onClick = { onTabChange(2) }
                                )
                            }
                            if (playerState.playbackSpeed != 1.0f || playerState.playbackPitch != 1.0f) {
                                TelemetryBadge(
                                    text = String.format(Locale.US, "%.2fx", playerState.playbackSpeed),
                                    color = AmberGlow,
                                    onClick = { showSpeedPitchDialog = true }
                                )
                            }
                            if (playerState.abLoopStartMs != null) {
                                val loopText = if (playerState.abLoopEndMs != null) {
                                    "Loop ${formatDuration(playerState.abLoopStartMs)}–${formatDuration(playerState.abLoopEndMs)}"
                                } else {
                                    "Loop A: ${formatDuration(playerState.abLoopStartMs)}"
                                }
                                TelemetryBadge(
                                    text = loopText,
                                    color = ElectricViolet,
                                    onClick = onClearAbLoop
                                )
                            }
                            if (playerState.sleepTimerRemainingMs != null) {
                                TelemetryBadge(
                                    text = "Sleep ${formatDuration(playerState.sleepTimerRemainingMs)}",
                                    color = BloomCoral,
                                    onClick = { showSleepTimerDialog = true }
                                )
                            } else if (playerState.sleepAtTrackEnd) {
                                TelemetryBadge(
                                    text = "Sleep: End of Song",
                                    color = BloomCoral,
                                    onClick = { showSleepTimerDialog = true }
                                )
                            }
                            if (playerState.isMuted) {
                                TelemetryBadge(
                                    text = "Muted",
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = onToggleMute
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (playerState.playbackError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = playerState.playbackError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onSmartReplace(track) }) {
                                        Text("Smart Replace")
                                    }
                                }
                            }
                        }

                        // Song info + Primary Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onToggleLike(track, isLiked) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Like track",
                                    tint = if (isLiked) BloomCoral else Color.White
                                )
                            }
                            IconButton(
                                onClick = { onAddToPlaylist(track) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = "Add to playlist",
                                    tint = SonicMint
                                )
                            }
                            IconButton(
                                onClick = { onSmartReplace(track) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = "Smart replace stream",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Seek Slider & Interactive Time Readout
                        val duration = playerState.durationMs.coerceAtLeast(1L)
                        Slider(
                            value = playerState.positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                            onValueChange = { onSeekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = BloomCoral,
                                activeTrackColor = BloomCoral,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(playerState.positionMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = if (showRemainingTime) {
                                    "-${formatDuration((duration - playerState.positionMs).coerceAtLeast(0L))}"
                                } else {
                                    formatDuration(playerState.durationMs)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showRemainingTime = !showRemainingTime }
                                    .padding(horizontal = 4.dp)
                            )
                        }

                        // Primary Transport Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playerState.shuffleEnabled) SonicMint else Color.White.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(BloomCoral)
                                    .clickable(onClick = onTogglePlayPause)
                                    .testTag("player_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playerState.isBuffering) {
                                    CircularProgressIndicator(
                                        color = ObsidianCanvas,
                                        modifier = Modifier.size(26.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        tint = ObsidianCanvas,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(
                                onClick = onCycleRepeat,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                                        Icons.Rounded.RepeatOne
                                    } else {
                                        Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Repeat mode",
                                    tint = if (playerState.repeatMode != RepeatMode.OFF) {
                                        SonicMint
                                    } else {
                                        Color.White.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }

                        // Advanced Audio Tools Dock
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("player_advanced_tools_dock")
                            ) {
                                item {
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.Replay10,
                                        label = "-10s",
                                        active = false,
                                        accent = Color.White,
                                        onClick = { onSeekBy(-10_000L) },
                                        testTag = "player_replay_10_button"
                                    )
                                }
                                item {
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.Forward10,
                                        label = "+10s",
                                        active = false,
                                        accent = Color.White,
                                        onClick = { onSeekBy(10_000L) },
                                        testTag = "player_forward_10_button"
                                    )
                                }
                                item {
                                    val speedChanged = playerState.playbackSpeed != 1.0f || playerState.playbackPitch != 1.0f
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.Speed,
                                        label = String.format(Locale.US, "%.2gx", playerState.playbackSpeed),
                                        active = speedChanged,
                                        accent = AmberGlow,
                                        onClick = { showSpeedPitchDialog = true },
                                        testTag = "player_speed_button"
                                    )
                                }
                                item {
                                    val abLabel = when {
                                        playerState.abLoopStartMs == null -> "A–B Loop"
                                        playerState.abLoopEndMs == null -> "Set B"
                                        else -> "Loop On"
                                    }
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.AllInclusive,
                                        label = abLabel,
                                        active = playerState.abLoopStartMs != null,
                                        accent = ElectricViolet,
                                        onClick = onToggleAbLoop,
                                        testTag = "player_ab_loop_button"
                                    )
                                }
                                item {
                                    val sleepActive = playerState.sleepTimerRemainingMs != null || playerState.sleepAtTrackEnd
                                    val sleepLabel = when {
                                        playerState.sleepTimerRemainingMs != null -> formatDuration(playerState.sleepTimerRemainingMs)
                                        playerState.sleepAtTrackEnd -> "End"
                                        else -> "Timer"
                                    }
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.Bedtime,
                                        label = sleepLabel,
                                        active = sleepActive,
                                        accent = BloomCoral,
                                        onClick = { showSleepTimerDialog = true },
                                        testTag = "player_sleep_timer_button"
                                    )
                                }
                                item {
                                    AdvancedToolButton(
                                        icon = if (playerState.isMuted) {
                                            Icons.AutoMirrored.Rounded.VolumeOff
                                        } else {
                                            Icons.AutoMirrored.Rounded.VolumeUp
                                        },
                                        label = if (playerState.isMuted) "Muted" else "${(playerState.playerVolume * 100).toInt()}%",
                                        active = playerState.isMuted || playerState.playerVolume < 0.99f,
                                        accent = SonicMint,
                                        onClick = { showVolumeDialog = true },
                                        testTag = "player_volume_button"
                                    )
                                }
                                item {
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.GraphicEq,
                                        label = "EQ Studio",
                                        active = playerState.eqEnabled && playerState.eqPreset != EqPreset.FLAT,
                                        accent = SonicMint,
                                        onClick = onOpenFullEqualizerScreen,
                                        testTag = "player_eq_studio_button"
                                    )
                                }
                                item {
                                    AdvancedToolButton(
                                        icon = Icons.Rounded.Share,
                                        label = "Share",
                                        active = false,
                                        accent = Color.White,
                                        onClick = {
                                            val shareText = "Listening to \"${track.title}\" by ${track.artist} on BloomeeTunes"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, track.title)
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            runCatching {
                                                context.startActivity(Intent.createChooser(intent, "Share Track"))
                                            }
                                        },
                                        testTag = "player_share_button"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Karaoke-Style Time-Synced Lyrics + Offset, Text Size & Copy Controls
                val synced = lyricsState.payload.syncedLines
                val adjustedPos = (playerState.positionMs + lyricsOffsetMs).coerceAtLeast(0L)
                val activeLineIdx = remember(adjustedPos, synced) {
                    synced.indexOfLast { it.timestampMs <= adjustedPos }
                }
                val listState = rememberLazyListState()
                LaunchedEffect(activeLineIdx) {
                    if (activeLineIdx >= 0) {
                        listState.animateScrollToItem((activeLineIdx - 2).coerceAtLeast(0))
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Karaoke Lyrics", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { largeLyricsText = !largeLyricsText },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FormatSize,
                                    contentDescription = "Toggle lyrics size",
                                    tint = if (largeLyricsText) SonicMint else Color.White
                                )
                            }
                            val copyableLyrics = remember(lyricsState.payload) {
                                if (synced.isNotEmpty()) synced.joinToString("\n") { it.text }
                                else lyricsState.payload.plainLyrics
                            }
                            if (copyableLyrics.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(copyableLyrics))
                                        onNotifyAction("Copied lyrics to clipboard")
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy lyrics",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Offset:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        TextButton(onClick = { onAdjustLyricsOffset(-250L) }) { Text("-0.25s") }
                        Text(
                            text = "${lyricsOffsetMs / 1000.0}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = SonicMint,
                            modifier = Modifier.clickable(onClick = onResetLyricsOffset)
                        )
                        TextButton(onClick = { onAdjustLyricsOffset(250L) }) { Text("+0.25s") }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (lyricsState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SonicMint)
                        }
                    } else if (synced.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(synced) { idx, line ->
                                val isCurrent = idx == activeLineIdx
                                Text(
                                    text = line.text,
                                    style = when {
                                        isCurrent && largeLyricsText -> MaterialTheme.typography.headlineLarge
                                        isCurrent -> MaterialTheme.typography.headlineMedium
                                        largeLyricsText -> MaterialTheme.typography.headlineMedium
                                        else -> MaterialTheme.typography.titleLarge
                                    },
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) SonicMint else Color.White.copy(alpha = 0.48f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSeekTo((line.timestampMs - lyricsOffsetMs).coerceAtLeast(0L))
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    } else if (lyricsState.payload.plainLyrics.isNotBlank()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = lyricsState.payload.plainLyrics,
                                    style = if (largeLyricsText) {
                                        MaterialTheme.typography.titleLarge
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = lyricsState.error ?: "No lyrics found for \"${track.title}\".",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        EqualizerStudioContent(
                            playerState = playerState,
                            onToggleEqEnabled = onToggleEqEnabled,
                            onApplyEqPreset = onApplyEqPreset,
                            onUpdateEqBand = onUpdateEqBand,
                            onBassBoostChange = onBassBoostChange,
                            onVirtualizerChange = onVirtualizerChange,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            onCrossfadeChange = onSetCrossfadeSeconds,
                            showHeaderToggle = true,
                            onOpenFullEqualizerScreen = onOpenFullEqualizerScreen
                        )
                    }
                }
            }

            3 -> {
                // Play Queue with Shuffle Upcoming, Move Up, Remove, and Clear Upcoming
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next (${playerState.queue.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onShuffleUpcomingQueue,
                                enabled = playerState.queue.size > 2,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("queue_shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle upcoming queue",
                                    tint = SonicMint
                                )
                            }
                            IconButton(
                                onClick = onClearUpcomingQueue,
                                enabled = playerState.queue.size > 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("queue_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Clear upcoming queue",
                                    tint = BloomCoral
                                )
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(playerState.queue) { idx, qTrack ->
                            val active = idx == playerState.currentIndex
                            Surface(
                                color = if (active) BloomCoral.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onPlayQueueItem(idx) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumArtImage(
                                        artworkUrl = qTrack.artworkUrl,
                                        contentDescription = qTrack.title,
                                        modifier = Modifier.size(44.dp),
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qTrack.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (active) BloomCoral else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = qTrack.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (active) {
                                        FilledTonalIconButton(
                                            onClick = onTogglePlayPause,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (playerState.isPlaying) "Pause" else "Play"
                                            )
                                        }
                                    } else {
                                        if (idx > 0 && idx - 1 != playerState.currentIndex) {
                                            IconButton(
                                                onClick = { onMoveQueueItem(idx, idx - 1) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowUpward,
                                                    contentDescription = "Move up in queue",
                                                    tint = Color.White.copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onRemoveQueueItem(idx) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove from queue",
                                                tint = Color.White.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpeedPitchDialog) {
        SpeedAndPitchDialog(
            speed = playerState.playbackSpeed,
            pitch = playerState.playbackPitch,
            onSpeedChange = onPlaybackSpeedChange,
            onPitchChange = onPlaybackPitchChange,
            onDismiss = { showSpeedPitchDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = playerState.sleepTimerRemainingMs,
            sleepAtTrackEnd = playerState.sleepAtTrackEnd,
            onSelectMinutes = { mins ->
                onSetSleepTimerMinutes(mins)
                showSleepTimerDialog = false
            },
            onSelectEndOfTrack = {
                onSetSleepAtTrackEnd()
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                onCancelSleepTimer()
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showVolumeDialog) {
        VolumeControlDialog(
            volume = playerState.playerVolume,
            isMuted = playerState.isMuted,
            onVolumeChange = onPlayerVolumeChange,
            onToggleMute = onToggleMute,
            onDismiss = { showVolumeDialog = false }
        )
    }
}

@Composable
private fun TelemetryBadge(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AdvancedToolButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .sizeIn(minWidth = 64.dp, minHeight = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) accent else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) accent else Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedAndPitchDialog(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speedPresets = listOf(0.75f, 0.85f, 1.0f, 1.25f, 1.5f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speed & Pitch Studio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    speedPresets.forEach { preset ->
                        FilterChip(
                            selected = kotlin.math.abs(speed - preset) < 0.03f,
                            onClick = { onSpeedChange(preset) },
                            label = { Text("${preset}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGlow.copy(alpha = 0.22f),
                                selectedLabelColor = AmberGlow
                            )
                        )
                    }
                    FilterChip(
                        selected = kotlin.math.abs(speed - 0.85f) < 0.03f && kotlin.math.abs(pitch - 0.90f) < 0.03f,
                        onClick = {
                            onSpeedChange(0.85f)
                            onPitchChange(0.90f)
                        },
                        label = { Text("Slowed Vibe") }
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tempo Speed", style = MaterialTheme.typography.titleMedium)
                        Text(String.format(Locale.US, "%.2fx", speed), color = AmberGlow)
                    }
                    Slider(
                        value = speed,
                        onValueChange = { raw ->
                            val stepped = (Math.round(raw * 20f) / 20f).coerceIn(0.5f, 2.0f)
                            onSpeedChange(stepped)
                        },
                        valueRange = 0.5f..2.0f
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vocal Pitch", style = MaterialTheme.typography.titleMedium)
                        Text(String.format(Locale.US, "%.2fx", pitch), color = SonicMint)
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { raw ->
                            val stepped = (Math.round(raw * 20f) / 20f).coerceIn(0.6f, 1.5f)
                            onPitchChange(stepped)
                        },
                        valueRange = 0.6f..1.5f
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSpeedChange(1.0f)
                onPitchChange(1.0f)
            }) {
                Text("Reset 1.0x")
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerDialog(
    remainingMs: Long?,
    sleepAtTrackEnd: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(10, 15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val statusText = when {
                    remainingMs != null -> "Active countdown: ${formatDuration(remainingMs)} remaining"
                    sleepAtTrackEnd -> "Playback will stop after the current song finishes."
                    else -> "Automatically fade out and pause playback after a duration."
                }
                Text(statusText, style = MaterialTheme.typography.bodyMedium)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { mins ->
                        OutlinedButton(onClick = { onSelectMinutes(mins) }) {
                            Text("$mins min")
                        }
                    }
                    FilledTonalButton(onClick = onSelectEndOfTrack) {
                        Text("End of Song")
                    }
                }
            }
        },
        dismissButton = {
            if (remainingMs != null || sleepAtTrackEnd) {
                TextButton(onClick = onCancelTimer) {
                    Text("Turn Off", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun VolumeControlDialog(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Gain & Mute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mute Audio", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = isMuted, onCheckedChange = { onToggleMute() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Output Gain", style = MaterialTheme.typography.bodyMedium)
                    Text("${(volume * 100).toInt()}%", color = SonicMint)
                }
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    enabled = !isMuted
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PlayerModePill(
    label: String,
    selected: Boolean,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) BloomCoral else Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) ObsidianCanvas else Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) ObsidianCanvas else Color.White
            )
        }
    }
}
