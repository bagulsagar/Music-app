package com.example

import com.example.data.local.BackupAndLocalMediaHelper
import com.example.data.model.SavedTrack
import com.example.data.repository.MusicCatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parseLrcLines_parsesSyncedTimestampsCorrectly() {
        val raw = """
            [00:12.50] First blossom line
            [01:05.00] Chorus drop
        """.trimIndent()
        val lines = MusicCatalogRepository.parseLrcLines(raw)
        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].timestampMs)
        assertEquals("First blossom line", lines[0].text)
        assertEquals(65_000L, lines[1].timestampMs)
    }

    @Test
    fun backupSerialization_roundTripsLikedTracks() {
        val track = SavedTrack(
            trackId = "itunes_123",
            title = "Neon Bloom",
            artist = "Skyline",
            album = "Afterglow",
            artworkUrl = "https://example.com/art.jpg",
            streamUrl = "https://example.com/preview.m4a",
            durationMs = 30_000L,
            genre = "Electronic"
        )
        val json = BackupAndLocalMediaHelper.serializeBackup(listOf(track), emptyList())
        val parsed = BackupAndLocalMediaHelper.parseBackup(json).getOrThrow()
        assertTrue(parsed.likedTracks.size == 1)
        assertEquals("Neon Bloom", parsed.likedTracks.first().title)
    }
}
