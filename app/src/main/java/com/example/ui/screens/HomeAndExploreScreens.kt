package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.HistoryEntry
import com.example.data.model.SavedTrack
import com.example.data.model.TrackItem
import com.example.data.repository.IndianSingerProfile
import com.example.data.repository.MusicCatalogRepository
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SonicMint
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.HomeAndExploreState
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    homeExplore: HomeAndExploreState,
    likedTracksState: UiState<List<SavedTrack>>,
    historyState: UiState<List<HistoryEntry>>,
    playlistsState: UiState<List<CollaborativePlaylist>>,
    isGuest: Boolean = false,
    currentTrackId: String?,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onPlayQueue: (List<TrackItem>) -> Unit,
    onToggleLike: (TrackItem, Boolean) -> Unit,
    onAddToPlaylist: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onSmartReplace: (TrackItem) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onNavigateToCollab: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSelectSinger: (IndianSingerProfile) -> Unit = {},
    onRequestSignIn: () -> Unit = {},
    onRetryCharts: () -> Unit
) {
    val likedTracks = (likedTracksState as? UiState.Success)?.data ?: emptyList()
    val likedIds = likedTracks.map { it.trackId }.toSet()
    val history = (historyState as? UiState.Success)?.data ?: emptyList()
    val playlists = (playlistsState as? UiState.Success)?.data ?: emptyList()

    val mostPlayed = history.sortedByDescending { it.playCount }.map { it.toTrackItem() }
    val recentTracks = history.map { it.toTrackItem() }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back to",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "BloomeeTunes",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenEqualizer)
                        .testTag("home_equalizer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Open Audio Equalizer",
                            tint = SonicMint
                        )
                    }
                }
            }
        }

        // Indian & Bollywood Singers Spotlight
        item {
            Column {
                Text(
                    text = "Indian & Bollywood Singers",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_singers_row")
                ) {
                    items(MusicCatalogRepository.INDIAN_SINGERS_SPOTLIGHT, key = { it.id }) { singer ->
                        IndianSingerSpotlightCard(
                            singer = singer,
                            onClick = { onSelectSinger(singer) }
                        )
                    }
                }
            }
        }

        // Smart Playlists Row (Recently Played, Most Played, Liked Gems)
        item {
            Column {
                Text(
                    text = "Smart Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SmartMixCard(
                            title = "Recently Played",
                            subtitle = "${recentTracks.size} tracks",
                            icon = Icons.Rounded.History,
                            accent = SonicMint,
                            onClick = {
                                if (recentTracks.isNotEmpty()) onPlayQueue(recentTracks)
                            }
                        )
                    }
                    item {
                        SmartMixCard(
                            title = "Most Played",
                            subtitle = "${mostPlayed.size} tracks",
                            icon = Icons.Rounded.AutoGraph,
                            accent = AmberGlow,
                            onClick = {
                                if (mostPlayed.isNotEmpty()) onPlayQueue(mostPlayed)
                            }
                        )
                    }
                    item {
                        SmartMixCard(
                            title = "Liked Gems",
                            subtitle = "${likedTracks.size} saved",
                            icon = Icons.Rounded.Favorite,
                            accent = BloomCoral,
                            onClick = {
                                if (likedTracks.isNotEmpty()) {
                                    onPlayQueue(likedTracks.map { it.toTrackItem() })
                                }
                            }
                        )
                    }
                }
            }
        }

        // Collaborative Playlists Spotlight
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collaborative Sessions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onNavigateToCollab) {
                        Text("See all")
                    }
                }
                if (isGuest) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onRequestSignIn)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(BloomCoral.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = BloomCoral
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guest Mode • Sign in for Live Collab",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap to sign in with Google and build real-time collaborative playlists with friends.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (playlists.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onNavigateToCollab)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SonicMint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = SonicMint
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start a Collaborative Playlist",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Share a 6-character invite code so friends can add & upvote tracks live.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists.take(6), key = { it.id }) { pl ->
                            val accent = parseHexColor(pl.accentColorHex)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .width(196.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onOpenPlaylist(pl.id) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(
                                                listOf(accent.copy(alpha = 0.28f), Color.Transparent)
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = accent.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = pl.inviteCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "${pl.memberUids.size} members",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = pl.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${pl.trackCount} tracks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bollywood & Indian Charts Carousel + Quick Picks
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bollywood & India Top Hits",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Hindi, Punjabi, Indie & Desi Singers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRetryCharts) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh charts")
                }
            }
        }

        if (homeExplore.chartsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (homeExplore.chartTracks.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = homeExplore.catalogError ?: "Connect to internet to load Bollywood & Indian charts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetryCharts) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(homeExplore.chartTracks.take(10), key = { it.trackId }) { track ->
                        Column(
                            modifier = Modifier
                                .width(148.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onPlayTrack(track, homeExplore.chartTracks) }
                        ) {
                            Box(modifier = Modifier.size(148.dp)) {
                                AlbumArtImage(
                                    artworkUrl = track.artworkUrl,
                                    contentDescription = "${track.title} album art",
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 18.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianCanvas.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play ${track.title}",
                                        tint = BloomCoral
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            items(homeExplore.chartTracks.drop(10), key = { it.trackId }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingNow = track.trackId == currentTrackId,
                    isLiked = track.trackId in likedIds,
                    onPlayClick = { onPlayTrack(track, homeExplore.chartTracks) },
                    onToggleLike = { onToggleLike(track, track.trackId in likedIds) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onSmartReplace = { onSmartReplace(track) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartMixCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.24f), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndianSingerSpotlightCard(
    singer: IndianSingerProfile,
    onClick: () -> Unit
) {
    val accent = parseHexColor(singer.accentHex, BloomCoral)
    val initials = singer.name
        .split(" ", ".")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("home_singer_card_${singer.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.24f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent, ElectricViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = singer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = singer.vibe,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ExploreScreen(
    homeExplore: HomeAndExploreState,
    likedIds: Set<String>,
    currentTrackId: String?,
    onSearchQueryChange: (String) -> Unit,
    onExecuteSearch: () -> Unit,
    onSelectGenre: (String) -> Unit,
    onSelectSinger: (IndianSingerProfile) -> Unit = {},
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleLike: (TrackItem, Boolean) -> Unit,
    onAddToPlaylist: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onSmartReplace: (TrackItem) -> Unit
) {
    val genres = MusicCatalogRepository.BOLLYWOOD_GENRE_CHIPS
    val singers = MusicCatalogRepository.INDIAN_SINGERS_SPOTLIGHT
    val displayList = if (homeExplore.searchQuery.isNotBlank() && homeExplore.searchResults.isNotEmpty()) {
        homeExplore.searchResults
    } else {
        homeExplore.chartTracks
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Bollywood & Indian Music",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = homeExplore.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search Bollywood songs or Indian singers...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (homeExplore.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onExecuteSearch() }),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { (queryKey, label) ->
                    val selected = homeExplore.selectedGenre == queryKey && homeExplore.searchQuery.isBlank()
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onSearchQueryChange("")
                            onSelectGenre(queryKey)
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Top Indian Singers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("explore_singers_row")
                ) {
                    items(singers, key = { it.id }) { singer ->
                        val selected = homeExplore.searchQuery.equals(singer.name, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSinger(singer) },
                            label = { Text(singer.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SonicMint.copy(alpha = 0.20f),
                                selectedLabelColor = SonicMint
                            ),
                            modifier = Modifier.testTag("explore_singer_chip_${singer.id}")
                        )
                    }
                }
            }
        }

        if (homeExplore.isSearching || homeExplore.chartsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        } else if (displayList.isEmpty()) {
            item {
                Text(
                    text = homeExplore.catalogError ?: "Search for any song to stream and add to playlists.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }
        } else {
            items(displayList, key = { it.trackId }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingNow = track.trackId == currentTrackId,
                    isLiked = track.trackId in likedIds,
                    onPlayClick = { onPlayTrack(track, displayList) },
                    onToggleLike = { onToggleLike(track, track.trackId in likedIds) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onSmartReplace = { onSmartReplace(track) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}
