package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class TrackItem(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val genre: String = "",
    val sourceType: String = SOURCE_STREAM,
    val accentHex: String = "#FF4D8D"
) {
    companion object {
        const val SOURCE_STREAM = "stream"
        const val SOURCE_LOCAL = "local"
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val genre: String = "",
    val sourceType: String = TrackItem.SOURCE_STREAM,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toTrackItem(): TrackItem = TrackItem(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        durationMs = durationMs,
        genre = genre,
        sourceType = sourceType
    )
}

@IgnoreExtraProperties
data class HistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val playCount: Long = 1L,
    @ServerTimestamp val playedAt: Timestamp? = null
) {
    fun toTrackItem(): TrackItem = TrackItem(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        durationMs = durationMs,
        genre = "History",
        sourceType = if (streamUrl.startsWith("content://")) TrackItem.SOURCE_LOCAL else TrackItem.SOURCE_STREAM
    )
}

@IgnoreExtraProperties
data class CollaborativePlaylist(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val accentColorHex: String = "#FF4D8D",
    val inviteCode: String = "",
    val isCollaborative: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val trackIds: List<String> = emptyList(),
    val trackCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlaylistInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val playlistId: String = "",
    val playlistTitle: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CollaborativePlaylistTrack(
    @DocumentId val id: String = "",
    val playlistId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val genre: String = "",
    val addedByUid: String = "",
    val addedByName: String = "",
    val upvotedBy: List<String> = emptyList(),
    val upvoteCount: Long = 0L,
    val orderIndex: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toTrackItem(accentHex: String = "#FF4D8D"): TrackItem = TrackItem(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        durationMs = durationMs,
        genre = genre,
        sourceType = if (streamUrl.startsWith("content://")) TrackItem.SOURCE_LOCAL else TrackItem.SOURCE_STREAM,
        accentHex = accentHex
    )
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsPayload(
    val syncedLines: List<LyricLine> = emptyList(),
    val plainLyrics: String = "",
    val source: String = "LRCLIB"
)

data class BackupDocument(
    val exportedAtEpochMs: Long,
    val appVersion: String = "BloomeeTunes-1.0",
    val likedTracks: List<BackupTrack> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList()
)

data class BackupTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val genre: String
)

data class BackupPlaylist(
    val title: String,
    val description: String,
    val accentColorHex: String,
    val isCollaborative: Boolean,
    val tracks: List<BackupTrack>
)
