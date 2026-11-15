package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.CollaborativePlaylistTrack
import com.example.data.model.HistoryEntry
import com.example.data.model.PlaylistInvite
import com.example.data.model.SavedTrack
import com.example.data.model.TrackItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class BloomeeFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentDisplayName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Bloomee Listener"
    }

    suspend fun ensureUserProfile(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        val displayName = currentDisplayName().take(80)
        val photoUrl = auth.currentUser?.photoUrl?.toString()?.take(500)
        if (!existing.exists()) {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("displayName", displayName)
                if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
        } else {
            val updates = buildMap<String, Any> {
                put("displayName", displayName)
                if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
        }
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeLikedTracks(userId: String): Flow<List<SavedTrack>> = flow {
        val path = "users/$userId/liked_tracks"
        emitAll(
            db.collection("users").document(userId).collection("liked_tracks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SavedTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun toggleLikeTrack(track: TrackItem, currentlyLiked: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val safeTrackId = sanitizeId(track.trackId)
        val docRef = db.collection("users").document(uid).collection("liked_tracks").document(safeTrackId)
        if (currentlyLiked) {
            docRef.delete().await()
            false
        } else {
            val payload = mapOf(
                "userId" to uid,
                "trackId" to safeTrackId,
                "title" to track.title.take(160).ifBlank { "Untitled" },
                "artist" to track.artist.take(120).ifBlank { "Unknown Artist" },
                "album" to track.album.take(160),
                "artworkUrl" to track.artworkUrl.take(600),
                "streamUrl" to track.streamUrl.take(1000),
                "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L),
                "genre" to track.genre.take(60),
                "sourceType" to if (track.sourceType == TrackItem.SOURCE_LOCAL) TrackItem.SOURCE_LOCAL else TrackItem.SOURCE_STREAM,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            true
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "users/${auth.currentUser?.uid}/liked_tracks", auth)
    }

    suspend fun updateLikedTrackStream(track: TrackItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeTrackId = sanitizeId(track.trackId)
        val docRef = db.collection("users").document(uid).collection("liked_tracks").document(safeTrackId)
        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "streamUrl" to track.streamUrl.take(1000),
                    "artworkUrl" to track.artworkUrl.take(600),
                    "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L)
                )
            ).await()
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "users/${auth.currentUser?.uid}/liked_tracks", auth)
    }

    fun observeHistory(userId: String): Flow<List<HistoryEntry>> = flow {
        val path = "users/$userId/history"
        emitAll(
            db.collection("users").document(userId).collection("history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(HistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.playedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun recordTrackPlayed(track: TrackItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeTrackId = sanitizeId(track.trackId)
        val docRef = db.collection("users").document(uid).collection("history").document(safeTrackId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "trackId" to safeTrackId,
                "title" to track.title.take(160).ifBlank { "Untitled" },
                "artist" to track.artist.take(120).ifBlank { "Unknown Artist" },
                "album" to track.album.take(160),
                "artworkUrl" to track.artworkUrl.take(600),
                "streamUrl" to track.streamUrl.take(1000),
                "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L),
                "playCount" to 1L,
                "playedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val currentCount = existing.getLong("playCount") ?: 1L
            docRef.update(
                mapOf(
                    "title" to track.title.take(160).ifBlank { "Untitled" },
                    "artist" to track.artist.take(120).ifBlank { "Unknown Artist" },
                    "album" to track.album.take(160),
                    "artworkUrl" to track.artworkUrl.take(600),
                    "streamUrl" to track.streamUrl.take(1000),
                    "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L),
                    "playCount" to (currentCount + 1L).coerceAtMost(1_000_000L),
                    "playedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "users/${auth.currentUser?.uid}/history", auth)
    }

    fun observePlaylists(userId: String): Flow<List<CollaborativePlaylist>> = flow {
        val path = "playlists"
        emitAll(
            db.collection("playlists")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CollaborativePlaylist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePlaylist(playlistId: String): Flow<CollaborativePlaylist?> = flow {
        val path = "playlists/$playlistId"
        emitAll(
            db.collection("playlists").document(playlistId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(CollaborativePlaylist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, auth.currentUser?.uid.orEmpty())
                    throw error
                }
        )
    }

    fun observePlaylistTracks(playlistId: String, userId: String): Flow<List<CollaborativePlaylistTrack>> = flow {
        val path = "playlists/$playlistId/tracks"
        emitAll(
            db.collection("playlists").document(playlistId).collection("tracks")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CollaborativePlaylistTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<CollaborativePlaylistTrack> { it.upvoteCount }.thenBy { it.orderIndex })
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    /**
     * When a new collaborator joins a playlist, pre-existing track docs may not yet include
     * their UID in denormalized `memberUids`. Fetch any missing IDs listed on `playlist.trackIds`
     * via single-doc `get()` (Parent Gate) and sync `memberUids` so real-time list updates work immediately.
     */
    suspend fun syncMissingPlaylistTracks(
        playlist: CollaborativePlaylist,
        currentlyVisibleIds: Set<String>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (uid !in playlist.memberUids) return@runCatching
        val missingIds = playlist.trackIds.filter { it !in currentlyVisibleIds }
        if (missingIds.isEmpty()) return@runCatching

        val tracksCol = db.collection("playlists").document(playlist.id).collection("tracks")
        for (trackDocId in missingIds) {
            val snap = tracksCol.document(trackDocId).get().await()
            if (snap.exists()) {
                val track = snap.toObject(CollaborativePlaylistTrack::class.java)
                if (track != null && !track.memberUids.containsAll(playlist.memberUids)) {
                    tracksCol.document(trackDocId).update(
                        mapOf(
                            "memberUids" to playlist.memberUids.take(25),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/${playlist.id}/tracks", auth)
    }

    suspend fun createPlaylist(
        title: String,
        description: String,
        accentColorHex: String,
        isCollaborative: Boolean
    ): Result<CollaborativePlaylist> = runCatching {
        val uid = requireUserId()
        val ownerName = currentDisplayName().take(80)
        val playlistRef = db.collection("playlists").document()
        val inviteCode = generateInviteCode()
        val cleanTitle = title.trim().take(80).ifBlank { "Untitled Mix" }
        val cleanDesc = description.trim().take(300)
        val cleanHex = if (Regex("^#[0-9A-Fa-f]{6}$").matches(accentColorHex)) accentColorHex else "#FF4D8D"

        val playlistPayload = mapOf(
            "ownerId" to uid,
            "ownerName" to ownerName,
            "title" to cleanTitle,
            "description" to cleanDesc,
            "accentColorHex" to cleanHex,
            "inviteCode" to inviteCode,
            "isCollaborative" to isCollaborative,
            "memberUids" to listOf(uid),
            "memberNames" to mapOf(uid to ownerName),
            "trackIds" to emptyList<String>(),
            "trackCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val invitePayload = mapOf(
            "inviteCode" to inviteCode,
            "playlistId" to playlistRef.id,
            "playlistTitle" to cleanTitle,
            "ownerId" to uid,
            "ownerName" to ownerName,
            "isActive" to isCollaborative,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(playlistRef, playlistPayload)
        batch.set(db.collection("playlist_invites").document(inviteCode), invitePayload)
        batch.commit().await()

        CollaborativePlaylist(
            id = playlistRef.id,
            ownerId = uid,
            ownerName = ownerName,
            title = cleanTitle,
            description = cleanDesc,
            accentColorHex = cleanHex,
            inviteCode = inviteCode,
            isCollaborative = isCollaborative,
            memberUids = listOf(uid),
            memberNames = mapOf(uid to ownerName),
            trackIds = emptyList(),
            trackCount = 0L
        )
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.CREATE, "playlists", auth)
    }

    suspend fun joinPlaylistByCode(rawCode: String): Result<String> = runCatching {
        val uid = requireUserId()
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        require(code.length in 6..16) { "Invite code must be 6–16 letters or numbers." }

        val inviteSnap = db.collection("playlist_invites").document(code).get().await()
        if (!inviteSnap.exists()) {
            throw IllegalArgumentException("No playlist found with invite code $code.")
        }
        val invite = inviteSnap.toObject(PlaylistInvite::class.java)
            ?: throw IllegalStateException("Invite details unavailable.")
        if (!invite.isActive) {
            throw IllegalStateException("This playlist is currently private and not accepting collaborators.")
        }

        val playlistRef = db.collection("playlists").document(invite.playlistId)
        // Check if caller is already a member (if so, get() succeeds directly)
        val alreadyMemberSnap = runCatching { playlistRef.get().await() }.getOrNull()
        if (alreadyMemberSnap != null && alreadyMemberSnap.exists()) {
            return@runCatching invite.playlistId
        }

        val myName = currentDisplayName().take(80)
        playlistRef.update(
            mapOf(
                "memberUids" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to myName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        invite.playlistId
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlist_invites/$rawCode", auth)
    }

    suspend fun addTrackToPlaylist(
        playlist: CollaborativePlaylist,
        track: TrackItem
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val myName = currentDisplayName().take(80)
        require(uid in playlist.memberUids) { "Join this playlist before adding tracks." }
        require(playlist.trackIds.size < 100) { "Playlist has reached the 100-track limit." }

        val playlistRef = db.collection("playlists").document(playlist.id)
        val trackDocRef = playlistRef.collection("tracks").document()
        val safeTrackId = sanitizeId(track.trackId)
        val members = playlist.memberUids.distinct().take(25)

        val trackPayload = mapOf(
            "playlistId" to playlist.id,
            "trackId" to safeTrackId,
            "title" to track.title.take(160).ifBlank { "Untitled" },
            "artist" to track.artist.take(120).ifBlank { "Unknown Artist" },
            "album" to track.album.take(160),
            "artworkUrl" to track.artworkUrl.take(600),
            "streamUrl" to track.streamUrl.take(1000),
            "durationMs" to track.durationMs.coerceIn(0L, 86_400_000L),
            "genre" to track.genre.take(60),
            "addedByUid" to uid,
            "addedByName" to myName,
            "upvotedBy" to listOf(uid),
            "upvoteCount" to 1L,
            "orderIndex" to playlist.trackCount.coerceIn(0L, 100_000L),
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val updatedTrackIds = (playlist.trackIds + trackDocRef.id).distinct().take(100)
        val batch = db.batch()
        batch.set(trackDocRef, trackPayload)
        batch.update(
            playlistRef,
            mapOf(
                "trackIds" to updatedTrackIds,
                "trackCount" to updatedTrackIds.size.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        trackDocRef.id
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.CREATE, "playlists/${playlist.id}/tracks", auth)
    }

    suspend fun toggleTrackUpvote(
        playlistId: String,
        trackItemId: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val trackRef = db.collection("playlists").document(playlistId)
            .collection("tracks").document(trackItemId)

        db.runTransaction { transaction ->
            val snap = transaction.get(trackRef)
            val currentUpvotes = (snap.get("upvotedBy") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList()
                ?: mutableListOf()
            if (uid in currentUpvotes) {
                currentUpvotes.remove(uid)
            } else if (currentUpvotes.size < 25) {
                currentUpvotes.add(uid)
            }
            transaction.update(
                trackRef,
                mapOf(
                    "upvotedBy" to currentUpvotes,
                    "upvoteCount" to currentUpvotes.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/$playlistId/tracks/$trackItemId", auth)
    }

    suspend fun removeTrackFromPlaylist(
        playlist: CollaborativePlaylist,
        trackItemId: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val playlistRef = db.collection("playlists").document(playlist.id)
        val trackRef = playlistRef.collection("tracks").document(trackItemId)
        val updatedTrackIds = playlist.trackIds.filterNot { it == trackItemId }

        val batch = db.batch()
        batch.delete(trackRef)
        batch.update(
            playlistRef,
            mapOf(
                "trackIds" to updatedTrackIds,
                "trackCount" to updatedTrackIds.size.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.DELETE, "playlists/${playlist.id}/tracks/$trackItemId", auth)
    }

    suspend fun setCollaborativeMode(
        playlist: CollaborativePlaylist,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(playlist.ownerId == uid) { "Only the playlist owner can change collaboration settings." }
        val playlistRef = db.collection("playlists").document(playlist.id)
        val inviteRef = db.collection("playlist_invites").document(playlist.inviteCode)

        val batch = db.batch()
        batch.update(
            playlistRef,
            mapOf(
                "isCollaborative" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            inviteRef,
            mapOf("isActive" to enabled)
        )
        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/${playlist.id}", auth)
    }

    suspend fun removeCollaborator(
        playlist: CollaborativePlaylist,
        memberUidToRemove: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(playlist.ownerId == uid) { "Only the owner can remove collaborators." }
        require(memberUidToRemove != playlist.ownerId) { "Owner cannot be removed from their own playlist." }

        val updatedMembers = playlist.memberUids.filterNot { it == memberUidToRemove }
        val updatedNames = playlist.memberNames.filterKeys { it != memberUidToRemove }
        db.collection("playlists").document(playlist.id).update(
            mapOf(
                "memberUids" to updatedMembers,
                "memberNames" to updatedNames,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/${playlist.id}", auth)
    }

    suspend fun leavePlaylist(playlist: CollaborativePlaylist): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(playlist.ownerId != uid) { "Owners must delete the playlist instead of leaving." }
        val updatedMembers = playlist.memberUids.filterNot { it == uid }
        val updatedNames = playlist.memberNames.filterKeys { it != uid }
        db.collection("playlists").document(playlist.id).update(
            mapOf(
                "memberUids" to updatedMembers,
                "memberNames" to updatedNames,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/${playlist.id}", auth)
    }

    suspend fun deletePlaylist(playlist: CollaborativePlaylist): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(playlist.ownerId == uid) { "Only the owner can delete this playlist." }
        val batch = db.batch()
        batch.delete(db.collection("playlists").document(playlist.id))
        batch.delete(db.collection("playlist_invites").document(playlist.inviteCode))
        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.DELETE, "playlists/${playlist.id}", auth)
    }

    suspend fun updatePlaylistTrackStream(
        playlistId: String,
        trackItemId: String,
        updatedTrack: TrackItem
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("playlists").document(playlistId)
            .collection("tracks").document(trackItemId)
            .update(
                mapOf(
                    "streamUrl" to updatedTrack.streamUrl.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "playlists/$playlistId/tracks/$trackItemId", auth)
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path, auth)
        }
    }

    companion object {
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun generateInviteCode(length: Int = 6): String =
            buildString(length) {
                repeat(length) {
                    append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)])
                }
            }

        fun sanitizeId(raw: String): String {
            val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(120)
            return cleaned.ifBlank { "trk_${System.currentTimeMillis()}" }
        }
    }
}
