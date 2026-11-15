package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BloomeeAudioEngine
import com.example.audio.EqPreset
import com.example.audio.PlayerUiState
import com.example.data.local.BackupAndLocalMediaHelper
import com.example.data.local.PreferencesManager
import com.example.data.local.UserPreferences
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.CollaborativePlaylistTrack
import com.example.data.model.HistoryEntry
import com.example.data.model.LyricsPayload
import com.example.data.model.SavedTrack
import com.example.data.model.TrackItem
import com.example.data.repository.BloomeeFirestoreRepository
import com.example.data.repository.IndianSingerProfile
import com.example.data.repository.MusicCatalogRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HomeAndExploreState(
    val chartsLoading: Boolean = true,
    val chartTracks: List<TrackItem> = emptyList(),
    val selectedGenre: String = MusicCatalogRepository.DEFAULT_BOLLYWOOD_GENRE,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<TrackItem> = emptyList(),
    val catalogError: String? = null
)

data class LyricsUiState(
    val trackId: String? = null,
    val isLoading: Boolean = false,
    val payload: LyricsPayload = LyricsPayload(),
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BloomeeViewModel(
    private val firestoreRepo: BloomeeFirestoreRepository?,
    private val catalogRepo: MusicCatalogRepository,
    private val audioEngine: BloomeeAudioEngine,
    private val preferencesManager: PreferencesManager,
    val currentUserId: String,
    val isGuest: Boolean = false
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = audioEngine.state

    val preferences: StateFlow<UserPreferences> = preferencesManager.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserPreferences()
        )

    private val _homeExplore = MutableStateFlow(HomeAndExploreState())
    val homeExplore: StateFlow<HomeAndExploreState> = _homeExplore.asStateFlow()

    private val _lyricsState = MutableStateFlow(LyricsUiState())
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _localTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val localTracks: StateFlow<List<TrackItem>> = _localTracks.asStateFlow()

    private val _guestLikedTracks = MutableStateFlow<List<SavedTrack>>(emptyList())
    private val _guestHistory = MutableStateFlow<List<HistoryEntry>>(emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)

    val likedTracksState: StateFlow<UiState<List<SavedTrack>>> =
        if (isGuest || firestoreRepo == null) {
            _guestLikedTracks
                .map<List<SavedTrack>, UiState<List<SavedTrack>>> { UiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            firestoreRepo.observeLikedTracks(currentUserId)
                .map<List<SavedTrack>, UiState<List<SavedTrack>>> { UiState.Success(it) }
                .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load liked tracks"))) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val historyState: StateFlow<UiState<List<HistoryEntry>>> =
        if (isGuest || firestoreRepo == null) {
            _guestHistory
                .map<List<HistoryEntry>, UiState<List<HistoryEntry>>> { UiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            firestoreRepo.observeHistory(currentUserId)
                .map<List<HistoryEntry>, UiState<List<HistoryEntry>>> { UiState.Success(it) }
                .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load listening history"))) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val playlistsState: StateFlow<UiState<List<CollaborativePlaylist>>> =
        if (isGuest || firestoreRepo == null) {
            flowOf<UiState<List<CollaborativePlaylist>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            firestoreRepo.observePlaylists(currentUserId)
                .map<List<CollaborativePlaylist>, UiState<List<CollaborativePlaylist>>> { UiState.Success(it) }
                .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load collaborative playlists"))) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    val selectedPlaylist: StateFlow<CollaborativePlaylist?> =
        if (isGuest || firestoreRepo == null) {
            flowOf<CollaborativePlaylist?>(null)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
        } else {
            _selectedPlaylistId
                .flatMapLatest { id ->
                    if (id == null) flowOf(null)
                    else firestoreRepo.observePlaylist(id)
                        .catch { error ->
                            Log.w(TAG, "Error observing playlist $id", error)
                            emit(null)
                        }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
        }

    val selectedPlaylistTracksState: StateFlow<UiState<List<CollaborativePlaylistTrack>>> =
        if (isGuest || firestoreRepo == null) {
            flowOf<UiState<List<CollaborativePlaylistTrack>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
        } else {
            _selectedPlaylistId
                .filterNotNull()
                .flatMapLatest { playlistId ->
                    firestoreRepo.observePlaylistTracks(playlistId, currentUserId)
                        .map<List<CollaborativePlaylistTrack>, UiState<List<CollaborativePlaylistTrack>>> {
                            UiState.Success(it)
                        }
                        .catch { error ->
                            emit(UiState.Error(unwrapErrorMessage(error, "Failed to observe playlist tracks")))
                        }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
        }

    init {
        if (!isGuest && firestoreRepo != null) {
            viewModelScope.launch {
                firestoreRepo.ensureUserProfile()
            }
        }
        viewModelScope.launch {
            val saved = preferencesManager.preferences.first()
            audioEngine.hydrateAudioSettings(
                eqEnabled = saved.eqEnabled,
                eqPreset = saved.eqPreset,
                customBandsMb = saved.eqCustomBandsMb,
                bassBoostStrength = saved.bassBoostStrength,
                virtualizerStrength = saved.virtualizerStrength,
                playbackSpeed = saved.playbackSpeed,
                crossfadeSeconds = saved.crossfadeSeconds
            )
        }
        audioEngine.onTrackStarted = { track ->
            if (isGuest || firestoreRepo == null) {
                recordGuestTrackPlayed(track)
            } else {
                viewModelScope.launch {
                    firestoreRepo.recordTrackPlayed(track)
                }
            }
            loadLyricsForTrack(track)
        }
        audioEngine.onPlaybackWarning = { warning ->
            postStatus(warning)
        }
        loadCharts(MusicCatalogRepository.DEFAULT_BOLLYWOOD_GENRE)
    }

    private fun recordGuestTrackPlayed(track: TrackItem) {
        _guestHistory.update { list ->
            val existing = list.firstOrNull { it.trackId == track.trackId }
            val updated = HistoryEntry(
                id = track.trackId,
                userId = currentUserId,
                trackId = track.trackId,
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                streamUrl = track.streamUrl,
                durationMs = track.durationMs,
                playCount = (existing?.playCount ?: 0L) + 1L
            )
            listOf(updated) + list.filterNot { it.trackId == track.trackId }.take(49)
        }
    }

    fun selectPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
    }

    fun syncSelectedPlaylistTracksIfNeeded(
        playlist: CollaborativePlaylist,
        visibleTracks: List<CollaborativePlaylistTrack>
    ) {
        val repo = firestoreRepo ?: return
        if (isGuest) return
        val visibleIds = visibleTracks.map { it.id }.toSet()
        if (playlist.trackIds.any { it !in visibleIds }) {
            viewModelScope.launch {
                repo.syncMissingPlaylistTracks(playlist, visibleIds)
            }
        }
    }

    fun loadCharts(genreTerm: String) {
        _homeExplore.update { it.copy(selectedGenre = genreTerm, chartsLoading = true, catalogError = null) }
        viewModelScope.launch {
            catalogRepo.fetchTopCharts(genreTerm)
                .onSuccess { tracks ->
                    _homeExplore.update {
                        it.copy(
                            chartsLoading = false,
                            chartTracks = tracks,
                            catalogError = if (tracks.isEmpty()) "No chart tracks available right now." else null
                        )
                    }
                }
                .onFailure { err ->
                    _homeExplore.update {
                        it.copy(
                            chartsLoading = false,
                            catalogError = err.localizedMessage ?: "Failed to load charts"
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _homeExplore.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _homeExplore.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun executeSearch(queryOverride: String? = null) {
        val q = (queryOverride ?: _homeExplore.value.searchQuery).trim()
        if (q.isEmpty()) return
        _homeExplore.update { it.copy(searchQuery = q, isSearching = true, catalogError = null) }
        viewModelScope.launch {
            catalogRepo.searchTracks(q)
                .onSuccess { results ->
                    _homeExplore.update {
                        it.copy(
                            isSearching = false,
                            searchResults = results,
                            catalogError = if (results.isEmpty()) "No songs matched \"$q\"." else null
                        )
                    }
                }
                .onFailure { err ->
                    _homeExplore.update {
                        it.copy(
                            isSearching = false,
                            catalogError = err.localizedMessage ?: "Search failed"
                        )
                    }
                }
        }
    }

    fun selectIndianSinger(singer: IndianSingerProfile) {
        _homeExplore.update {
            it.copy(
                searchQuery = singer.name,
                isSearching = true,
                catalogError = null
            )
        }
        viewModelScope.launch {
            catalogRepo.searchTracks(singer.searchQuery)
                .onSuccess { results ->
                    _homeExplore.update {
                        it.copy(
                            isSearching = false,
                            searchResults = results,
                            catalogError = if (results.isEmpty()) "No songs found for ${singer.name}." else null
                        )
                    }
                }
                .onFailure { err ->
                    _homeExplore.update {
                        it.copy(
                            isSearching = false,
                            catalogError = err.localizedMessage ?: "Failed to load ${singer.name} tracks"
                        )
                    }
                }
        }
    }

    fun playTrack(track: TrackItem, contextQueue: List<TrackItem> = emptyList()) {
        if (contextQueue.isNotEmpty()) {
            val idx = contextQueue.indexOfFirst { it.trackId == track.trackId && it.streamUrl == track.streamUrl }
            if (idx >= 0) {
                audioEngine.playQueue(contextQueue, idx)
                return
            }
        }
        audioEngine.playSingle(track)
    }

    fun playQueue(tracks: List<TrackItem>, startIndex: Int = 0) {
        audioEngine.playQueue(tracks, startIndex)
    }

    fun addToQueue(track: TrackItem) {
        audioEngine.addToQueue(track)
        postStatus("Added \"${track.title}\" to queue")
    }

    fun togglePlayPause() = audioEngine.togglePlayPause()
    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = audioEngine.seekBy(deltaMs)
    fun skipNext() = audioEngine.skipNext(fromAutoComplete = false)
    fun skipPrevious() = audioEngine.skipPrevious()
    fun toggleShuffle() = audioEngine.toggleShuffle()
    fun cycleRepeatMode() = audioEngine.cycleRepeatMode()
    fun setPlaybackPitch(pitch: Float) = audioEngine.setPlaybackPitch(pitch)
    fun setPlayerVolume(volume: Float) = audioEngine.setPlayerVolume(volume)
    fun toggleMute() = audioEngine.toggleMute()

    fun toggleAbLoopPoint() {
        audioEngine.toggleAbLoopPoint()
        val st = audioEngine.state.value
        val msg = when {
            st.abLoopStartMs != null && st.abLoopEndMs == null -> "Loop start (A) set"
            st.abLoopStartMs != null && st.abLoopEndMs != null -> "A–B Loop active"
            else -> "A–B Loop cleared"
        }
        postStatus(msg)
    }

    fun clearAbLoop() {
        audioEngine.clearAbLoop()
        postStatus("A–B Loop cleared")
    }

    fun setSleepTimerMinutes(minutes: Int) {
        audioEngine.setSleepTimerMinutes(minutes)
        postStatus("Sleep timer set for $minutes min")
    }

    fun setSleepAtEndOfTrack() {
        audioEngine.setSleepAtEndOfTrack()
        postStatus("Playback will pause after this song")
    }

    fun cancelSleepTimer() {
        audioEngine.cancelSleepTimer()
        postStatus("Sleep timer cancelled")
    }

    fun removeFromQueue(index: Int) = audioEngine.removeFromQueue(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = audioEngine.moveQueueItem(fromIndex, toIndex)
    fun clearUpcomingQueue() {
        audioEngine.clearUpcomingQueue()
        postStatus("Cleared upcoming queue")
    }
    fun shuffleUpcomingQueue() {
        audioEngine.shuffleUpcomingQueue()
        postStatus("Shuffled upcoming queue")
    }

    fun toggleEqEnabled(enabled: Boolean) {
        audioEngine.toggleEqEnabled(enabled)
        viewModelScope.launch {
            val st = audioEngine.state.value
            preferencesManager.setEqSettings(enabled, st.eqPreset, st.eqBands.map { it.levelMb })
        }
    }

    fun applyEqPreset(preset: EqPreset) {
        audioEngine.applyEqPreset(preset)
        viewModelScope.launch {
            val st = audioEngine.state.value
            preferencesManager.setEqSettings(true, preset, st.eqBands.map { it.levelMb })
        }
    }

    fun updateEqBand(index: Short, levelMb: Short) {
        audioEngine.updateEqBand(index, levelMb)
        viewModelScope.launch {
            val st = audioEngine.state.value
            preferencesManager.setEqSettings(true, EqPreset.CUSTOM, st.eqBands.map { it.levelMb })
        }
    }

    fun setBassBoostStrength(strength: Int) {
        audioEngine.setBassBoostStrength(strength)
        viewModelScope.launch { preferencesManager.setBassBoostStrength(strength) }
    }

    fun setVirtualizerStrength(strength: Int) {
        audioEngine.setVirtualizerStrength(strength)
        viewModelScope.launch { preferencesManager.setVirtualizerStrength(strength) }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioEngine.setPlaybackSpeed(speed)
        viewModelScope.launch { preferencesManager.setPlaybackSpeed(speed) }
    }

    fun resetAudioEffects() {
        audioEngine.resetAudioEffects()
        viewModelScope.launch {
            preferencesManager.setEqSettings(true, EqPreset.FLAT, listOf(0, 0, 0, 0, 0))
            preferencesManager.setBassBoostStrength(0)
            preferencesManager.setVirtualizerStrength(0)
            preferencesManager.setPlaybackSpeed(1.0f)
            preferencesManager.setCrossfadeSeconds(3)
        }
        postStatus("Equalizer & playback settings reset to Balanced")
    }

    fun setCrossfadeSeconds(seconds: Int) {
        audioEngine.setCrossfadeSeconds(seconds)
        viewModelScope.launch { preferencesManager.setCrossfadeSeconds(seconds) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkTheme(enabled) }
    }

    fun setDynamicAccent(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDynamicAccent(enabled) }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val updated = (preferences.value.lyricsOffsetMs + deltaMs).coerceIn(-5000L, 5000L)
        viewModelScope.launch { preferencesManager.setLyricsOffsetMs(updated) }
    }

    fun resetLyricsOffset() {
        viewModelScope.launch { preferencesManager.setLyricsOffsetMs(0L) }
    }

    fun loadLyricsForTrack(track: TrackItem) {
        if (_lyricsState.value.trackId == track.trackId &&
            (_lyricsState.value.payload.syncedLines.isNotEmpty() || _lyricsState.value.payload.plainLyrics.isNotBlank())
        ) {
            return
        }
        _lyricsState.value = LyricsUiState(trackId = track.trackId, isLoading = true)
        viewModelScope.launch {
            catalogRepo.fetchLyrics(track.artist, track.title)
                .onSuccess { payload ->
                    _lyricsState.value = LyricsUiState(
                        trackId = track.trackId,
                        isLoading = false,
                        payload = payload,
                        error = if (payload.syncedLines.isEmpty() && payload.plainLyrics.isBlank()) {
                            "No synced lyrics found for this track yet."
                        } else null
                    )
                }
                .onFailure {
                    _lyricsState.value = LyricsUiState(
                        trackId = track.trackId,
                        isLoading = false,
                        error = "Lyrics unavailable while offline."
                    )
                }
        }
    }

    fun smartReplaceTrack(
        track: TrackItem,
        playlistId: String? = null,
        playlistTrackItemId: String? = null
    ) {
        viewModelScope.launch {
            postStatus("Finding fresh stream for \"${track.title}\"...")
            catalogRepo.smartReplaceTrack(track)
                .onSuccess { healed ->
                    audioEngine.replaceTrackInQueue(track.trackId, healed)
                    if (isGuest || firestoreRepo == null) {
                        _guestLikedTracks.update { list ->
                            list.map {
                                if (it.trackId == healed.trackId) {
                                    it.copy(streamUrl = healed.streamUrl, durationMs = healed.durationMs)
                                } else it
                            }
                        }
                    } else {
                        firestoreRepo.updateLikedTrackStream(healed)
                        if (playlistId != null && playlistTrackItemId != null) {
                            firestoreRepo.updatePlaylistTrackStream(playlistId, playlistTrackItemId, healed)
                        }
                    }
                    postStatus("Smart Replace healed \"${track.title}\"")
                }
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Smart Replace could not find an alternative stream")
                }
        }
    }

    fun toggleLikeTrack(track: TrackItem, currentlyLiked: Boolean) {
        if (isGuest || firestoreRepo == null) {
            _guestLikedTracks.update { current ->
                if (currentlyLiked) {
                    current.filterNot { it.trackId == track.trackId }
                } else {
                    val saved = SavedTrack(
                        trackId = track.trackId,
                        userId = currentUserId,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        genre = track.genre
                    )
                    listOf(saved) + current.filterNot { it.trackId == track.trackId }
                }
            }
            postStatus(if (!currentlyLiked) "Saved to Liked Songs (Guest)" else "Removed from Liked Songs")
            return
        }
        viewModelScope.launch {
            firestoreRepo.toggleLikeTrack(track, currentlyLiked)
                .onSuccess { liked ->
                    postStatus(if (liked) "Saved to Liked Songs" else "Removed from Liked Songs")
                }
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Failed to update liked songs")
                }
        }
    }

    fun createPlaylist(
        title: String,
        description: String,
        accentColorHex: String,
        isCollaborative: Boolean,
        onCreated: (CollaborativePlaylist) -> Unit = {}
    ) {
        val repo = firestoreRepo
        if (isGuest || repo == null) {
            postStatus("Sign in with Google to create collaborative playlists")
            return
        }
        viewModelScope.launch {
            repo.createPlaylist(title, description, accentColorHex, isCollaborative)
                .onSuccess { created ->
                    postStatus("Created \"${created.title}\" (Code: ${created.inviteCode})")
                    onCreated(created)
                }
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Failed to create playlist")
                }
        }
    }

    fun joinPlaylistByCode(code: String, onJoined: (String) -> Unit = {}) {
        val repo = firestoreRepo
        if (isGuest || repo == null) {
            postStatus("Sign in with Google to join collaborative playlists")
            return
        }
        viewModelScope.launch {
            repo.joinPlaylistByCode(code)
                .onSuccess { playlistId ->
                    postStatus("Joined collaborative playlist!")
                    onJoined(playlistId)
                }
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Invalid or inactive invite code")
                }
        }
    }

    fun addTrackToPlaylist(playlist: CollaborativePlaylist, track: TrackItem) {
        val repo = firestoreRepo
        if (isGuest || repo == null) {
            postStatus("Sign in with Google to save tracks to collaborative playlists")
            return
        }
        viewModelScope.launch {
            repo.addTrackToPlaylist(playlist, track)
                .onSuccess {
                    postStatus("Added \"${track.title}\" to ${playlist.title}")
                }
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Could not add track to playlist")
                }
        }
    }

    fun toggleTrackUpvote(playlistId: String, trackItemId: String) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.toggleTrackUpvote(playlistId, trackItemId)
                .onFailure { err ->
                    postStatus(err.localizedMessage ?: "Upvote failed")
                }
        }
    }

    fun removeTrackFromPlaylist(playlist: CollaborativePlaylist, trackItemId: String) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.removeTrackFromPlaylist(playlist, trackItemId)
                .onSuccess { postStatus("Removed track from ${playlist.title}") }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Could not remove track") }
        }
    }

    fun setCollaborativeMode(playlist: CollaborativePlaylist, enabled: Boolean) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.setCollaborativeMode(playlist, enabled)
                .onSuccess {
                    postStatus(if (enabled) "Invite code enabled" else "Playlist set to private")
                }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Failed to update settings") }
        }
    }

    fun removeCollaborator(playlist: CollaborativePlaylist, memberUid: String) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.removeCollaborator(playlist, memberUid)
                .onSuccess { postStatus("Collaborator removed") }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Could not remove collaborator") }
        }
    }

    fun leavePlaylist(playlist: CollaborativePlaylist, onLeft: () -> Unit = {}) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.leavePlaylist(playlist)
                .onSuccess {
                    postStatus("Left \"${playlist.title}\"")
                    onLeft()
                }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Could not leave playlist") }
        }
    }

    fun deletePlaylist(playlist: CollaborativePlaylist, onDeleted: () -> Unit = {}) {
        val repo = firestoreRepo
        if (isGuest || repo == null) return
        viewModelScope.launch {
            repo.deletePlaylist(playlist)
                .onSuccess {
                    postStatus("Deleted \"${playlist.title}\"")
                    onDeleted()
                }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Could not delete playlist") }
        }
    }

    fun importLocalAudioUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = uris.map { uri ->
                BackupAndLocalMediaHelper.extractLocalTrackMetadata(context, uri)
            }
            _localTracks.update { existing ->
                val existingUrls = existing.map { it.streamUrl }.toSet()
                existing + imported.filter { it.streamUrl !in existingUrls }
            }
            postStatus("Imported ${imported.size} local track(s) to Library")
        }
    }

    fun exportLibraryBackup(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            val liked = (likedTracksState.value as? UiState.Success)?.data ?: emptyList()
            val playlists = (playlistsState.value as? UiState.Success)?.data ?: emptyList()
            val json = BackupAndLocalMediaHelper.serializeBackup(
                likedTracks = liked,
                playlistsWithTracks = playlists.map { it to emptyList() }
            )
            BackupAndLocalMediaHelper.writeTextToUri(context, destinationUri, json)
                .onSuccess { postStatus("Exported BloomeeTunes JSON backup (${liked.size} liked, ${playlists.size} playlists)") }
                .onFailure { err -> postStatus(err.localizedMessage ?: "Export failed") }
        }
    }

    fun importLibraryBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            val raw = BackupAndLocalMediaHelper.readTextFromUri(context, sourceUri)
                .getOrElse {
                    postStatus(it.localizedMessage ?: "Could not read backup file")
                    return@launch
                }
            val doc = BackupAndLocalMediaHelper.parseBackup(raw)
                .getOrElse {
                    postStatus(it.localizedMessage ?: "Invalid JSON backup format")
                    return@launch
                }

            var restoredCount = 0
            doc.likedTracks.forEach { t ->
                val track = TrackItem(
                    trackId = t.trackId,
                    title = t.title,
                    artist = t.artist,
                    album = t.album,
                    artworkUrl = t.artworkUrl,
                    streamUrl = t.streamUrl,
                    durationMs = t.durationMs,
                    genre = t.genre
                )
                if (isGuest || firestoreRepo == null) {
                    val saved = SavedTrack(
                        trackId = track.trackId,
                        userId = currentUserId,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        genre = track.genre
                    )
                    _guestLikedTracks.update { list ->
                        if (list.any { it.trackId == saved.trackId }) list else list + saved
                    }
                    restoredCount++
                } else if (firestoreRepo.toggleLikeTrack(track, currentlyLiked = false).isSuccess) {
                    restoredCount++
                }
            }
            if (!isGuest && firestoreRepo != null) {
                doc.playlists.forEach { bp ->
                    val created = firestoreRepo.createPlaylist(
                        title = bp.title,
                        description = bp.description,
                        accentColorHex = bp.accentColorHex,
                        isCollaborative = bp.isCollaborative
                    ).getOrNull()
                    if (created != null) {
                        bp.tracks.forEach { t ->
                            firestoreRepo.addTrackToPlaylist(
                                created,
                                TrackItem(
                                    trackId = t.trackId,
                                    title = t.title,
                                    artist = t.artist,
                                    album = t.album,
                                    artworkUrl = t.artworkUrl,
                                    streamUrl = t.streamUrl,
                                    durationMs = t.durationMs,
                                    genre = t.genre
                                )
                            )
                        }
                    }
                }
                postStatus("Restored $restoredCount liked tracks & ${doc.playlists.size} playlists")
            } else {
                postStatus("Restored $restoredCount liked tracks to Guest session")
            }
        }
    }

    fun postStatus(message: String) {
        _statusMessage.value = message
    }

    fun consumeStatus() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    private fun unwrapErrorMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return root.message ?: fallback
    }

    private companion object {
        const val TAG = "BloomeeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
