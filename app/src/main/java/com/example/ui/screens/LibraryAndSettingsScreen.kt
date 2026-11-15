package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.model.HistoryEntry
import com.example.data.model.SavedTrack
import com.example.data.model.TrackItem
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SonicMint
import com.example.ui.viewmodel.UiState

@Composable
fun LibraryScreen(
    likedTracksState: UiState<List<SavedTrack>>,
    historyState: UiState<List<HistoryEntry>>,
    localTracks: List<TrackItem>,
    preferences: UserPreferences,
    isGuest: Boolean = false,
    currentTrackId: String?,
    userEmailOrName: String,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleLike: (TrackItem, Boolean) -> Unit,
    onAddToPlaylist: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onSmartReplace: (TrackItem) -> Unit,
    onImportLocalUris: (List<android.net.Uri>) -> Unit,
    onExportBackup: (android.net.Uri) -> Unit,
    onImportBackup: (android.net.Uri) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleDynamicAccent: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onRequestSignIn: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Liked, 1=Local, 2=History, 3=Settings & Backup

    val localAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        onImportLocalUris(uris)
    }

    val backupExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val backupImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    val likedTracks = (likedTracksState as? UiState.Success)?.data?.map { it.toTrackItem() } ?: emptyList()
    val likedIds = likedTracks.map { it.trackId }.toSet()
    val historyTracks = (historyState as? UiState.Success)?.data?.map { it.toTrackItem() } ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Your Library", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = userEmailOrName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Liked", "Local", "History", "Backup").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (likedTracks.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "No liked songs yet",
                            body = "Tap the heart icon on any track in Home or Explore to save it to your cloud library."
                        )
                    }
                } else {
                    items(likedTracks, key = { it.trackId }) { track ->
                        TrackRowItem(
                            track = track,
                            isPlayingNow = track.trackId == currentTrackId,
                            isLiked = true,
                            onPlayClick = { onPlayTrack(track, likedTracks) },
                            onToggleLike = { onToggleLike(track, true) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onSmartReplace = { onSmartReplace(track) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
            1 -> {
                item {
                    Button(
                        onClick = { localAudioPicker.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonicMint,
                            contentColor = ObsidianCanvas
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp)
                            .testTag("import_local_audio_button")
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Audio Files from Device")
                    }
                }
                if (localTracks.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "Blend local files with online streams",
                            body = "Select MP3, M4A, or FLAC files from your device using Android's zero-permission document picker."
                        )
                    }
                } else {
                    items(localTracks, key = { it.trackId }) { track ->
                        TrackRowItem(
                            track = track,
                            isPlayingNow = track.trackId == currentTrackId,
                            isLiked = track.trackId in likedIds,
                            onPlayClick = { onPlayTrack(track, localTracks) },
                            onToggleLike = { onToggleLike(track, track.trackId in likedIds) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onSmartReplace = { onSmartReplace(track) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
            2 -> {
                if (historyTracks.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            title = "Listening history is empty",
                            body = "Tracks you play automatically appear here and feed your Smart Playlists."
                        )
                    }
                } else {
                    items(historyTracks, key = { it.trackId }) { track ->
                        TrackRowItem(
                            track = track,
                            isPlayingNow = track.trackId == currentTrackId,
                            isLiked = track.trackId in likedIds,
                            onPlayClick = { onPlayTrack(track, historyTracks) },
                            onToggleLike = { onToggleLike(track, track.trackId in likedIds) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onSmartReplace = { onSmartReplace(track) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
            3 -> {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Backup, contentDescription = null, tint = SonicMint)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Portable JSON Backups", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Export your liked songs and playlists to a portable .json file, or restore a backup anytime.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { backupExporter.launch("bloomeetunes_backup.json") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export")
                                    }
                                    OutlinedButton(
                                        onClick = { backupImporter.launch(arrayOf("application/json", "text/plain")) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import")
                                    }
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = SonicMint)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Audio Equalizer & Playback", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Tune 5-band frequencies, Bass Boost, 3D Spatializer, tempo, and track crossfade.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenEqualizer,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SonicMint,
                                        contentColor = ObsidianCanvas
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("library_open_equalizer_button")
                                ) {
                                    Icon(Icons.Rounded.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Audio Equalizer")
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("Appearance & Audio Theme", style = MaterialTheme.typography.titleLarge)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = BloomCoral)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Nocturnal Dark Mode", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Switch(checked = preferences.darkTheme, onCheckedChange = onToggleDarkTheme)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = SonicMint)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Dynamic Track Accent", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Switch(checked = preferences.dynamicArtworkAccent, onCheckedChange = onToggleDynamicAccent)
                                }
                            }
                        }

                        if (isGuest) {
                            Button(
                                onClick = onRequestSignIn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BloomCoral,
                                    contentColor = ObsidianCanvas
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("guest_sign_in_button")
                            ) {
                                Text("Sign in with Google")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryNotice(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
