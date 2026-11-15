package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.CollaborativePlaylistTrack
import com.example.data.model.TrackItem
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.PlaylistAccentPresets
import com.example.ui.theme.SonicMint
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.UiState

@Composable
fun CollabHubScreen(
    playlistsState: UiState<List<CollaborativePlaylist>>,
    isGuest: Boolean = false,
    onOpenPlaylist: (String) -> Unit,
    onShowCreateDialog: () -> Unit,
    onShowJoinDialog: () -> Unit,
    onRequestSignIn: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(172.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_collab_banner),
                    contentDescription = "Collaborative playlists banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(ObsidianCanvas.copy(alpha = 0.25f), ObsidianCanvas.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Collaborative Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Build shared mixes in real time, invite friends with a code, and upvote tracks together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (isGuest) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = null,
                            tint = SonicMint,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Sign in to Collaborate Live",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "You're currently listening in Guest mode. Sign in with Google to create shared playlists, join invite codes, and sync likes across devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestSignIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BloomCoral,
                                contentColor = ObsidianCanvas
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("collab_guest_sign_in_button")
                        ) {
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShowCreateDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloomCoral,
                            contentColor = ObsidianCanvas
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("create_collab_playlist_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Mix", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShowJoinDialog,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("join_by_code_button")
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, tint = SonicMint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join Code", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            when (playlistsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SonicMint)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = playlistsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val playlists = playlistsState.data
                    if (playlists.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GroupAdd,
                                        contentDescription = null,
                                        tint = SonicMint,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No collaborative playlists yet",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap Create Mix to start a live room or Join Code to hop into a friend's playlist.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                    items(playlists, key = { it.id }) { playlist ->
                        val accent = parseHexColor(playlist.accentColorHex)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onOpenPlaylist(playlist.id) }
                                .testTag("playlist_card_${playlist.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent.copy(alpha = 0.22f), Color.Transparent)
                                        )
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accent.copy(alpha = 0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (playlist.isCollaborative) Icons.Rounded.Group else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "by ${playlist.ownerName} • ${playlist.trackCount} tracks • ${playlist.memberUids.size} members",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = playlist.inviteCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, accentHex: String, isCollaborative: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedHex by remember { mutableStateOf(PlaylistAccentPresets.first()) }
    var isCollaborative by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Vibe / Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Accent Bloom", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlaylistAccentPresets.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedHex == hex) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedHex == hex) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected color",
                                    tint = ObsidianCanvas,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Collaborative Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Generate shareable invite code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCollaborative,
                        onCheckedChange = { isCollaborative = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, selectedHex, isCollaborative)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinByCodeDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Collaborative Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the 6-character invite code shared by the playlist creator.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
                    label = { Text("Invite Code (e.g. BLM49K)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_invite_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.length >= 6) onJoin(code) },
                enabled = code.length >= 6,
                modifier = Modifier.testTag("confirm_join_code_button")
            ) {
                Text("Join Mix")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: CollaborativePlaylist?,
    tracksState: UiState<List<CollaborativePlaylistTrack>>,
    currentUserId: String,
    currentPlayingTrackId: String?,
    chartTracks: List<TrackItem>,
    searchResults: List<TrackItem>,
    searchQuery: String,
    isSearching: Boolean,
    onBack: () -> Unit,
    onSyncMissingTracks: (CollaborativePlaylist, List<CollaborativePlaylistTrack>) -> Unit,
    onPlayTracks: (List<TrackItem>, Int) -> Unit,
    onToggleUpvote: (String) -> Unit,
    onRemoveTrack: (CollaborativePlaylist, String) -> Unit,
    onSmartReplacePlaylistTrack: (CollaborativePlaylist, CollaborativePlaylistTrack) -> Unit,
    onAddTrackToPlaylist: (CollaborativePlaylist, TrackItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExecuteSearch: () -> Unit,
    onToggleCollaborativeMode: (CollaborativePlaylist, Boolean) -> Unit,
    onRemoveCollaborator: (CollaborativePlaylist, String) -> Unit,
    onLeavePlaylist: (CollaborativePlaylist) -> Unit,
    onDeletePlaylist: (CollaborativePlaylist) -> Unit,
    onCopyInviteCode: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val clipboard = LocalClipboardManager.current
    var showAddTrackSheet by remember { mutableStateOf(false) }

    val tracks = (tracksState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(playlist?.id, playlist?.trackIds, tracks.size) {
        if (playlist != null) {
            onSyncMissingTracks(playlist, tracks)
        }
    }

    if (playlist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val accent = parseHexColor(playlist.accentColorHex)
    val isOwner = playlist.ownerId == currentUserId

    LazyColumn(
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.34f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isOwner) {
                            IconButton(
                                onClick = { onToggleCollaborativeMode(playlist, !playlist.isCollaborative) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (playlist.isCollaborative) Icons.Rounded.Public else Icons.Rounded.Lock,
                                    contentDescription = "Toggle collaborative mode",
                                    tint = accent
                                )
                            }
                            IconButton(
                                onClick = { onDeletePlaylist(playlist) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete playlist",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onLeavePlaylist(playlist) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ExitToApp,
                                    contentDescription = "Leave playlist"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Invite Code Share Pill + Play/Add Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                clipboard.setText(AnnotatedString(playlist.inviteCode))
                                onCopyInviteCode(playlist.inviteCode)
                            }
                            .testTag("copy_invite_code_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy invite code",
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (playlist.isCollaborative) "CODE: ${playlist.inviteCode}" else "PRIVATE",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = { showAddTrackSheet = true },
                        modifier = Modifier.testTag("add_track_to_collab_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Song")
                    }

                    Button(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                onPlayTracks(tracks.map { it.toTrackItem(playlist.accentColorHex) }, 0)
                            }
                        },
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianCanvas)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Collaborators Strip
                Text(
                    text = "Collaborators (${playlist.memberUids.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlist.memberUids) { uid ->
                        val name = playlist.memberNames[uid] ?: "Collaborator"
                        val isCreator = uid == playlist.ownerId
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isCreator) "$name (Host)" else name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isOwner && !isCreator) {
                                    Text(
                                        text = "×",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { onRemoveCollaborator(playlist, uid) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This collaborative mix is empty", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Tap Add Song or share code ${playlist.inviteCode} so friends can drop tracks!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(tracks, key = { it.id }) { pTrack ->
                val isUpvoted = currentUserId in pTrack.upvotedBy
                val canDelete = isOwner || pTrack.addedByUid == currentUserId
                val asTrack = pTrack.toTrackItem(playlist.accentColorHex)
                val queue = tracks.map { it.toTrackItem(playlist.accentColorHex) }
                val idx = tracks.indexOf(pTrack).coerceAtLeast(0)

                Surface(
                    color = if (pTrack.trackId == currentPlayingTrackId) {
                        accent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onPlayTracks(queue, idx) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            artworkUrl = pTrack.artworkUrl,
                            contentDescription = "${pTrack.title} cover",
                            modifier = Modifier.size(52.dp),
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pTrack.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pTrack.artist} • ${formatDuration(pTrack.durationMs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Added by ${pTrack.addedByName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }

                        // Upvote button + count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isUpvoted) accent.copy(alpha = 0.22f) else Color.Transparent
                                )
                                .clickable { onToggleUpvote(pTrack.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("upvote_track_${pTrack.id}")
                        ) {
                            Icon(
                                imageVector = if (isUpvoted) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                                contentDescription = "Upvote track",
                                tint = if (isUpvoted) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pTrack.upvoteCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isUpvoted) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onSmartReplacePlaylistTrack(playlist, pTrack) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = "Smart replace stream",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (canDelete) {
                            IconButton(
                                onClick = { onRemoveTrack(playlist, pTrack.id) },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove track",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTrackSheet) {
        val candidates = if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) searchResults else chartTracks
        ModalBottomSheet(
            onDismissRequest = { showAddTrackSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Add Song to ${playlist.title}", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search Bollywood, Hindi, or Indian singer...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onExecuteSearch() }),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(candidates, key = { it.trackId }) { track ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onAddTrackToPlaylist(playlist, track)
                                        showAddTrackSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumArtImage(
                                        artworkUrl = track.artworkUrl,
                                        contentDescription = track.title,
                                        modifier = Modifier.size(44.dp),
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Icon(Icons.Rounded.Add, contentDescription = "Add", tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
