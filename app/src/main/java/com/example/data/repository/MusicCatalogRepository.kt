package com.example.data.repository

import android.util.Log
import com.example.data.model.LyricLine
import com.example.data.model.LyricsPayload
import com.example.data.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class IndianSingerProfile(
    val id: String,
    val name: String,
    val vibe: String,
    val searchQuery: String,
    val accentHex: String
)

class MusicCatalogRepository(
    private val client: OkHttpClient = defaultClient()
) {
    @Volatile
    private var cachedDiscoveryHost: String? = null

    suspend fun fetchTopCharts(genreTerm: String = DEFAULT_BOLLYWOOD_GENRE): Result<List<TrackItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = resolveDiscoveryBase()
                val normalized = genreTerm.trim().lowercase()
                val queryPlan = when (normalized) {
                    "", "top hits", "all", DEFAULT_BOLLYWOOD_GENRE -> listOf(
                        "Bollywood",
                        "Hindi",
                        "Arijit Singh",
                        "Pritam",
                        "Shreya Ghoshal"
                    )
                    "arijit romantic", "arijit singh" -> listOf(
                        "Arijit Singh",
                        "Atif Aslam",
                        "Jubin Nautiyal",
                        "Shreya Ghoshal"
                    )
                    "punjabi" -> listOf(
                        "Punjabi",
                        "Diljit Dosanjh",
                        "AP Dhillon",
                        "Karan Aujla"
                    )
                    "hindi lofi", "lofi beats", "lo-fi", "lofi" -> listOf(
                        "Bollywood Lofi",
                        "Hindi Lofi",
                        "Slowed Reverb Hindi"
                    )
                    "desi hip hop", "hip hop", "hip-hop" -> listOf(
                        "Desi Hip Hop",
                        "Hindi Rap",
                        "Divine",
                        "Krsna"
                    )
                    "indian indie", "indie chill", "indie" -> listOf(
                        "Indian Indie",
                        "Anuv Jain",
                        "Prateek Kuhad",
                        "Hindi Indie"
                    )
                    "sufi rahman", "sufi classical", "acoustic" -> listOf(
                        "AR Rahman",
                        "Sufi Hindi",
                        "Bollywood Acoustic",
                        "Qawwali"
                    )
                    "bollywood party", "electronic synthwave", "electronic" -> listOf(
                        "Bollywood Remix",
                        "Bollywood Dance",
                        "Badshah",
                        "Neha Kakkar"
                    )
                    else -> listOf("$genreTerm Bollywood", genreTerm)
                }

                val aggregated = aggregateCuratedQueries(base, queryPlan, perQueryLimit = 12)
                if (aggregated.isNotEmpty()) {
                    aggregated
                } else {
                    val trendingUrl = "$base/v1/tracks/trending?limit=30&time=week&app_name=$APP_NAME"
                    parseAudiusTrackList(executeGet(trendingUrl), base)
                }
            }.onFailure {
                cachedDiscoveryHost = null
                Log.w(TAG, "Chart fetch failed for $genreTerm", it)
            }
        }

    suspend fun searchTracks(query: String, limit: Int = 25): Result<List<TrackItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()
                val primary = searchTracksInternal(trimmed, limit)
                if (primary.isNotEmpty()) {
                    primary
                } else {
                    searchTracksInternal("$trimmed Hindi", limit)
                }
            }.onFailure {
                cachedDiscoveryHost = null
                Log.w(TAG, "Search failed for '$query'", it)
            }
        }

    private fun aggregateCuratedQueries(
        baseHost: String,
        queries: List<String>,
        perQueryLimit: Int
    ): List<TrackItem> {
        val seenIds = LinkedHashSet<String>()
        val collected = ArrayList<TrackItem>()
        for (term in queries) {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = "$baseHost/v1/tracks/search?query=$encoded&limit=$perQueryLimit&app_name=$APP_NAME"
            val parsed = runCatching { parseAudiusTrackList(executeGet(url), baseHost) }
                .getOrDefault(emptyList())
            for (track in parsed) {
                if (seenIds.add(track.trackId)) {
                    collected.add(track)
                }
            }
        }
        // Prioritize full-length tracks (>= 60s) with artwork so playback & visuals stay crisp
        return collected.sortedWith(
            compareByDescending<TrackItem> { it.durationMs >= 60_000L }
                .thenByDescending { it.artworkUrl.isNotBlank() }
        ).take(40)
    }

    /**
     * BloomeeTunes Smart Replace: finds a fresh full-length working stream
     * matching the track's title and artist on Audius.
     */
    suspend fun smartReplaceTrack(track: TrackItem): Result<TrackItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = "${track.artist} ${track.title}".trim()
                val candidates = searchTracksInternal(query, limit = 10)
                val match = candidates.firstOrNull {
                    it.streamUrl.isNotBlank() &&
                        (it.title.contains(track.title, ignoreCase = true) ||
                            track.title.contains(it.title, ignoreCase = true))
                } ?: candidates.firstOrNull { it.streamUrl.isNotBlank() }
                ?: throw IllegalStateException("No full-length alternative stream found for ${track.title}")

                track.copy(
                    streamUrl = match.streamUrl,
                    artworkUrl = track.artworkUrl.ifBlank { match.artworkUrl },
                    durationMs = if (match.durationMs > 0L) match.durationMs else track.durationMs
                )
            }
        }

    /**
     * Fetches time-synced LRC or plain lyrics from LRCLIB (open-source, no API key required).
     */
    suspend fun fetchLyrics(artist: String, title: String): Result<LyricsPayload> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanArtist = artist.substringBefore(",").substringBefore("feat.").trim()
                val cleanTitle = title.substringBefore("(").substringBefore("-").trim()
                val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")

                val getUrl = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"
                val directJson = runCatching { JSONObject(executeGet(getUrl)) }.getOrNull()
                if (directJson != null && !directJson.has("statusCode")) {
                    return@runCatching parseLrcJson(directJson)
                }

                val q = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
                val searchUrl = "https://lrclib.net/api/search?q=$q"
                val arr = JSONArray(executeGet(searchUrl))
                if (arr.length() > 0) {
                    return@runCatching parseLrcJson(arr.getJSONObject(0))
                }
                LyricsPayload()
            }
        }

    private fun resolveDiscoveryBase(): String {
        cachedDiscoveryHost?.let { return it }
        val discovered = runCatching {
            val root = JSONObject(executeGet("https://api.audius.co"))
            val hosts = root.optJSONArray("data")
            if (hosts != null && hosts.length() > 0) {
                hosts.optString(0).trimEnd('/')
            } else null
        }.getOrNull()
        val chosen = if (!discovered.isNullOrBlank() && discovered.startsWith("https://")) {
            discovered
        } else {
            "https://api.audius.co"
        }
        cachedDiscoveryHost = chosen
        return chosen
    }

    private fun searchTracksInternal(term: String, limit: Int): List<TrackItem> {
        val base = resolveDiscoveryBase()
        val encoded = URLEncoder.encode(term, "UTF-8")
        val url = "$base/v1/tracks/search?query=$encoded&limit=$limit&app_name=$APP_NAME"
        return parseAudiusTrackList(executeGet(url), base)
    }

    private fun parseAudiusTrackList(body: String, baseHost: String): List<TrackItem> =
        Companion.parseAudiusTrackList(body, baseHost)

    private fun parseLrcJson(json: JSONObject): LyricsPayload {
        val synced = json.optString("syncedLyrics", "")
        val plain = json.optString("plainLyrics", "")
        val lines = parseLrcLines(synced)
        return LyricsPayload(
            syncedLines = lines,
            plainLyrics = plain,
            source = "LRCLIB"
        )
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BloomeeTunes-Android/1.0")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from $url")
            }
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val TAG = "MusicCatalogRepo"
        private const val APP_NAME = "BloomeeTunes"
        const val DEFAULT_BOLLYWOOD_GENRE = "bollywood top"

        val BOLLYWOOD_GENRE_CHIPS = listOf(
            "bollywood top" to "Bollywood Hits",
            "arijit romantic" to "Arijit & Romantic",
            "punjabi" to "Punjabi Hits",
            "hindi lofi" to "Hindi Lo-Fi",
            "desi hip hop" to "Desi Hip-Hop",
            "indian indie" to "Indian Indie",
            "sufi rahman" to "Sufi & Rahman",
            "bollywood party" to "Party & Remix"
        )

        val INDIAN_SINGERS_SPOTLIGHT = listOf(
            IndianSingerProfile("arijit", "Arijit Singh", "Romantic & Soul", "Arijit Singh", "#FF4D8D"),
            IndianSingerProfile("shreya", "Shreya Ghoshal", "Melody Queen", "Shreya Ghoshal", "#00D9B5"),
            IndianSingerProfile("rahman", "A.R. Rahman", "Maestro & Sufi", "AR Rahman", "#8B5CF6"),
            IndianSingerProfile("diljit", "Diljit Dosanjh", "Punjabi Pop", "Diljit Dosanjh", "#F59E0B"),
            IndianSingerProfile("atif", "Atif Aslam", "Evergreen Hits", "Atif Aslam", "#3B82F6"),
            IndianSingerProfile("pritam", "Pritam", "Film Blockbusters", "Pritam Bollywood", "#EC4899"),
            IndianSingerProfile("jubin", "Jubin Nautiyal", "Love & Acoustic", "Jubin Nautiyal", "#10B981"),
            IndianSingerProfile("anuv", "Anuv Jain", "Indian Indie", "Anuv Jain", "#F97316"),
            IndianSingerProfile("ap_dhillon", "AP Dhillon", "Desi Wave", "AP Dhillon", "#A855F7"),
            IndianSingerProfile("sunidhi", "Sunidhi Chauhan", "Power Vocals", "Sunidhi Chauhan", "#EF4444"),
            IndianSingerProfile("sonu", "Sonu Nigam", "Classic Bollywood", "Sonu Nigam", "#14B8A6"),
            IndianSingerProfile("divine", "DIVINE", "Desi Hip-Hop", "DIVINE Indian Rap", "#EAB308")
        )

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        internal fun parseAudiusTrackList(body: String, baseHost: String): List<TrackItem> {
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: return emptyList()
            val tracks = ArrayList<TrackItem>(data.length())
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (id.isBlank()) continue
                if (obj.has("is_streamable") && !obj.optBoolean("is_streamable", true)) continue

                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue

                val durationSec = obj.optLong("duration", 0L)
                if (durationSec <= 0L) continue

                val userObj = obj.optJSONObject("user") ?: obj.optJSONObject("artist")
                val artistName = userObj?.optString("name", "")?.takeIf { it.isNotBlank() }
                    ?: userObj?.optString("handle", "")?.takeIf { it.isNotBlank() }
                    ?: "Independent Artist"

                val artworkObj = obj.optJSONObject("artwork")
                val artworkUrl = artworkObj?.optString("480x480", "")
                    ?.ifBlank { artworkObj.optString("1000x1000", "") }
                    ?.ifBlank { artworkObj.optString("150x150", "") }
                    .orEmpty()

                val genre = obj.optString("genre", "").ifBlank { "Music" }
                val mood = obj.optString("mood", "")
                val albumLabel = if (mood.isNotBlank()) "$genre • $mood" else genre

                tracks.add(
                    TrackItem(
                        trackId = "audius_$id",
                        title = title,
                        artist = artistName,
                        album = albumLabel,
                        artworkUrl = artworkUrl,
                        streamUrl = "$baseHost/v1/tracks/$id/stream?app_name=$APP_NAME",
                        durationMs = durationSec * 1000L,
                        genre = genre,
                        sourceType = TrackItem.SOURCE_STREAM
                    )
                )
            }
            return tracks
        }

        fun parseLrcLines(lrcText: String): List<LyricLine> {
            if (lrcText.isBlank()) return emptyList()
            val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")
            val parsed = mutableListOf<LyricLine>()
            lrcText.lineSequence().forEach { line ->
                val match = regex.find(line.trim()) ?: return@forEach
                val mins = match.groupValues[1].toLongOrNull() ?: 0L
                val secs = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val millis = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    parsed.add(
                        LyricLine(
                            timestampMs = mins * 60_000L + secs * 1_000L + millis,
                            text = text
                        )
                    )
                }
            }
            return parsed.sortedBy { it.timestampMs }
        }
    }
}
