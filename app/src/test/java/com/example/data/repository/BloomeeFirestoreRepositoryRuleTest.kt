package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TrackItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BloomeeFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleTrack(suffix: String = UUID.randomUUID().toString().take(6)) = TrackItem(
        trackId = "itunes_$suffix",
        title = "Midnight Bloom $suffix",
        artist = "Bloomee Collective",
        album = "Sonic Garden",
        artworkUrl = "https://example.com/art.jpg",
        streamUrl = "https://example.com/preview.m4a",
        durationMs = 30_000L,
        genre = "Electronic",
        sourceType = TrackItem.SOURCE_STREAM
    )

    @Test
    fun createPlaylist_andAddTrack_emitsRealtimePlaylistAndTracks() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = BloomeeFirestoreRepository(firestore, auth)
        repo.ensureUserProfile().getOrThrow()

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPlaylist(
                title = "Late Night Drive",
                description = "Chill synthwave",
                accentColorHex = "#FF4D8D",
                isCollaborative = true
            ).getOrThrow()
        }
        assertTrue(created.id.isNotBlank())
        assertTrue(created.inviteCode.length == 6)

        val track = sampleTrack()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTrackToPlaylist(created, track).getOrThrow()
        }

        val playlists = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePlaylists(aliceUid).first { list -> list.any { it.id == created.id } }
        }
        assertTrue(playlists.any { it.id == created.id })

        val tracks = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePlaylistTracks(created.id, aliceUid).first { list -> list.isNotEmpty() }
        }
        assertEquals(track.title, tracks.first().title)
    }

    @Test
    fun joinPlaylistByCode_collaboratorCanSyncPreExistingTracksAddAndUpvote() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = BloomeeFirestoreRepository(firestore, auth)
        val playlist = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPlaylist(
                title = "Shared Jam",
                description = "Open collab",
                accentColorHex = "#38E8C6",
                isCollaborative = true
            ).getOrThrow()
        }
        val firstTrackId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addTrackToPlaylist(playlist, sampleTrack("first")).getOrThrow()
        }

        // Bob joins via invite code
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = BloomeeFirestoreRepository(firestore, auth)
        val joinedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinPlaylistByCode(playlist.inviteCode).getOrThrow()
        }
        assertEquals(playlist.id, joinedId)

        val bobPlaylist = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePlaylist(joinedId).first { it != null && bobUid in it.memberUids }!!
        }
        // Bob syncs pre-existing track added by Alice before Bob joined
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncMissingPlaylistTracks(bobPlaylist, emptySet()).getOrThrow()
        }

        val bobTracks = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePlaylistTracks(joinedId, bobUid).first { list -> list.any { it.id == firstTrackId } }
        }
        assertTrue(bobTracks.any { it.id == firstTrackId })

        // Bob upvotes Alice's track and adds his own track
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleTrackUpvote(joinedId, firstTrackId).getOrThrow()
            bobRepo.addTrackToPlaylist(bobPlaylist, sampleTrack("bob")).getOrThrow()
        }

        val updatedTracks = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePlaylistTracks(joinedId, bobUid).first { it.size == 2 }
        }
        assertEquals(2, updatedTracks.size)
    }

    @Test
    fun observePlaylists_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BloomeeFirestoreRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePlaylists("someone_else").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = caught!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun toggleLikeTrack_crossUserReadDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BloomeeFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.toggleLikeTrack(sampleTrack("liked"), currentlyLiked = false).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BloomeeFirestoreRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeLikedTracks(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = caught!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_bloomee@test.com"
        const val BOB_EMAIL = "bob_bloomee@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
