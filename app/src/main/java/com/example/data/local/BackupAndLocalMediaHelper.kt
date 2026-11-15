package com.example.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.BackupDocument
import com.example.data.model.BackupPlaylist
import com.example.data.model.BackupTrack
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.CollaborativePlaylistTrack
import com.example.data.model.SavedTrack
import com.example.data.model.TrackItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object BackupAndLocalMediaHelper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(BackupDocument::class.java).indent("  ")

    fun serializeBackup(
        likedTracks: List<SavedTrack>,
        playlistsWithTracks: List<Pair<CollaborativePlaylist, List<CollaborativePlaylistTrack>>>
    ): String {
        val doc = BackupDocument(
            exportedAtEpochMs = System.currentTimeMillis(),
            likedTracks = likedTracks.map {
                BackupTrack(
                    trackId = it.trackId,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    artworkUrl = it.artworkUrl,
                    streamUrl = it.streamUrl,
                    durationMs = it.durationMs,
                    genre = it.genre
                )
            },
            playlists = playlistsWithTracks.map { (pl, tracks) ->
                BackupPlaylist(
                    title = pl.title,
                    description = pl.description,
                    accentColorHex = pl.accentColorHex,
                    isCollaborative = pl.isCollaborative,
                    tracks = tracks.map { t ->
                        BackupTrack(
                            trackId = t.trackId,
                            title = t.title,
                            artist = t.artist,
                            album = t.album,
                            artworkUrl = t.artworkUrl,
                            streamUrl = t.streamUrl,
                            durationMs = t.durationMs,
                            genre = t.genre
                        )
                    }
                )
            }
        )
        return backupAdapter.toJson(doc)
    }

    fun parseBackup(jsonString: String): Result<BackupDocument> = runCatching {
        backupAdapter.fromJson(jsonString) ?: error("Invalid BloomeeTunes backup file")
    }

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open destination file")
                Unit
            }
        }

    suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Could not read selected file")
            }
        }

    suspend fun extractLocalTrackMetadata(context: Context, uri: Uri): TrackItem =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            var title = ""
            var artist = ""
            var album = ""
            var durationMs = 0L
            runCatching {
                retriever.setDataSource(context, uri)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty()
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty()
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            }
            runCatching { retriever.release() }

            if (title.isBlank()) {
                title = queryDisplayName(context, uri).substringBeforeLast(".")
                    .ifBlank { "Local Audio" }
            }

            TrackItem(
                trackId = "local_${UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString().take(12)}",
                title = title.take(160),
                artist = artist.ifBlank { "Local Device" }.take(120),
                album = album.ifBlank { "Imported Audio" }.take(160),
                artworkUrl = "",
                streamUrl = uri.toString(),
                durationMs = durationMs.coerceAtLeast(0L),
                genre = "Local",
                sourceType = TrackItem.SOURCE_LOCAL
            )
        }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx).orEmpty()
                }
            }
        return uri.lastPathSegment.orEmpty()
    }
}
