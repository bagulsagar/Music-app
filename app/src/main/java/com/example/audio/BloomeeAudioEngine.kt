package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class RepeatMode { OFF, ALL, ONE }

enum class EqPreset(val label: String, val gainsMb: List<Short>) {
    FLAT("Balanced", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Bloom", listOf(550, 350, 0, 100, 200)),
    VOCAL("Crystal Vocal", listOf(-150, 100, 450, 400, 150)),
    NIGHT_CLUB("Midnight Club", listOf(450, 250, -100, 300, 400)),
    ACOUSTIC("Warm Acoustic", listOf(200, 150, 250, 150, 100)),
    ROCK("Arena Rock", listOf(400, 200, -100, 300, 450)),
    ELECTRONIC("Synth Wave", listOf(500, 300, 50, 250, 500)),
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0))
}

data class EqBandInfo(
    val index: Short,
    val centerFreqHz: Int,
    val levelMb: Short
)

data class PlayerUiState(
    val currentTrack: TrackItem? = null,
    val queue: List<TrackItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val crossfadeSeconds: Int = 3,
    val eqEnabled: Boolean = true,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqBands: List<EqBandInfo> = defaultEqBands(),
    val eqMinMb: Short = -1500,
    val eqMaxMb: Short = 1500,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val playerVolume: Float = 1.0f,
    val isMuted: Boolean = false,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val sleepTimerRemainingMs: Long? = null,
    val sleepAtTrackEnd: Boolean = false,
    val playbackError: String? = null
)

private fun defaultEqBands(): List<EqBandInfo> = listOf(
    EqBandInfo(0, 60, 0),
    EqBandInfo(1, 230, 0),
    EqBandInfo(2, 910, 0),
    EqBandInfo(3, 3600, 0),
    EqBandInfo(4, 14000, 0)
)

@OptIn(UnstableApi::class)
class BloomeeAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val mediaDataSourceFactory by lazy {
        val okHttpFactory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent("BloomeeTunes-Android/1.0")
        DefaultDataSource.Factory(appContext, okHttpFactory)
    }

    private val mediaAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private var activePlayer: ExoPlayer? = null
    private var fadingOutPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var progressJob: Job? = null
    private var fadeInJob: Job? = null
    private var fadeOutJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeTriggeredForTrackId: String? = null
    private var startedNotifiedTrackId: String? = null
    private var mutedVolumeWarningShown = false

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    var onTrackStarted: ((TrackItem) -> Unit)? = null
    var onPlaybackWarning: ((String) -> Unit)? = null

    fun playQueue(tracks: List<TrackItem>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        _state.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIdx,
                playbackError = null
            )
        }
        startTrackInternal(tracks[safeIdx], crossfade = false)
    }

    fun playSingle(track: TrackItem) {
        val existingQueue = _state.value.queue
        val idx = existingQueue.indexOfFirst { it.trackId == track.trackId && it.streamUrl == track.streamUrl }
        if (idx >= 0) {
            _state.update { it.copy(currentIndex = idx, playbackError = null) }
            startTrackInternal(existingQueue[idx], crossfade = false)
        } else {
            val updated = listOf(track) + existingQueue
            _state.update { it.copy(queue = updated, currentIndex = 0, playbackError = null) }
            startTrackInternal(track, crossfade = false)
        }
    }

    fun addToQueue(track: TrackItem) {
        _state.update { current ->
            if (current.queue.any { it.trackId == track.trackId && it.streamUrl == track.streamUrl }) {
                current
            } else {
                val updated = current.queue + track
                current.copy(queue = updated)
            }
        }
        if (_state.value.currentTrack == null) {
            playQueue(_state.value.queue, 0)
        }
    }

    fun togglePlayPause() {
        val player = activePlayer
        val currentTrack = _state.value.currentTrack ?: return
        if (player == null) {
            startTrackInternal(currentTrack, crossfade = false)
            return
        }
        runCatching {
            if (player.isPlaying) {
                player.pause()
                _state.update { it.copy(isPlaying = false) }
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                } else if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                applyEffectiveVolume(player)
                player.play()
                warnIfDeviceMediaVolumeMuted()
                _state.update { it.copy(isPlaying = true) }
            }
        }.onFailure {
            startTrackInternal(currentTrack, crossfade = false)
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _state.value.durationMs.coerceAtLeast(1L)
        val target = positionMs.coerceIn(0L, dur)
        _state.update { it.copy(positionMs = target) }
        val player = activePlayer ?: return
        runCatching {
            player.seekTo(target)
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (_state.value.positionMs + deltaMs).coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        seekTo(target)
    }

    fun skipNext(fromAutoComplete: Boolean = false) {
        val current = _state.value
        if (current.queue.isEmpty()) return

        if (fromAutoComplete && current.repeatMode == RepeatMode.ONE) {
            current.currentTrack?.let { startTrackInternal(it, crossfade = false) }
            return
        }

        val nextIdx = when {
            current.shuffleEnabled && current.queue.size > 1 -> {
                val candidates = current.queue.indices.filter { it != current.currentIndex }
                candidates.random()
            }
            current.currentIndex + 1 < current.queue.size -> current.currentIndex + 1
            current.repeatMode == RepeatMode.ALL -> 0
            else -> {
                if (fromAutoComplete) {
                    _state.update { it.copy(isPlaying = false, positionMs = 0L) }
                    return
                }
                0
            }
        }
        _state.update { it.copy(currentIndex = nextIdx, playbackError = null) }
        startTrackInternal(
            current.queue[nextIdx],
            crossfade = _state.value.crossfadeSeconds > 0 && fromAutoComplete
        )
    }

    fun skipPrevious() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        if (current.positionMs > 3000L) {
            seekTo(0L)
            return
        }
        val prevIdx = if (current.currentIndex - 1 >= 0) {
            current.currentIndex - 1
        } else {
            current.queue.lastIndex
        }
        _state.update { it.copy(currentIndex = prevIdx, playbackError = null) }
        startTrackInternal(current.queue[prevIdx], crossfade = false)
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _state.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _state.update { it.copy(crossfadeSeconds = seconds.coerceIn(0, 8)) }
    }

    fun hydrateAudioSettings(
        eqEnabled: Boolean,
        eqPreset: EqPreset,
        customBandsMb: List<Short>,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
        playbackSpeed: Float,
        crossfadeSeconds: Int
    ) {
        val currentBands = _state.value.eqBands
        val minMb = _state.value.eqMinMb
        val maxMb = _state.value.eqMaxMb
        val targetGains = if (eqPreset == EqPreset.CUSTOM && customBandsMb.isNotEmpty()) {
            customBandsMb
        } else {
            eqPreset.gainsMb
        }
        val hydratedBands = currentBands.mapIndexed { idx, band ->
            band.copy(levelMb = targetGains.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb))
        }
        val safeBass = bassBoostStrength.coerceIn(0, 1000)
        val safeVirt = virtualizerStrength.coerceIn(0, 1000)
        val safeSpeed = playbackSpeed.coerceIn(0.5f, 2.0f)
        _state.update {
            it.copy(
                eqEnabled = eqEnabled,
                eqPreset = eqPreset,
                eqBands = hydratedBands,
                bassBoostStrength = safeBass,
                virtualizerStrength = safeVirt,
                playbackSpeed = safeSpeed,
                crossfadeSeconds = crossfadeSeconds.coerceIn(0, 8)
            )
        }
        syncHardwareEffectsToCurrentPlayer()
        applyPlaybackParams(activePlayer, safeSpeed, _state.value.playbackPitch)
    }

    fun toggleEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
        syncHardwareEffectsToCurrentPlayer()
    }

    fun applyEqPreset(preset: EqPreset) {
        val currentBands = _state.value.eqBands
        val updatedBands = currentBands.mapIndexed { idx, band ->
            val gain = preset.gainsMb.getOrElse(idx) { 0 }
                .coerceIn(_state.value.eqMinMb, _state.value.eqMaxMb)
            band.copy(levelMb = gain)
        }
        _state.update { it.copy(eqPreset = preset, eqBands = updatedBands, eqEnabled = true) }
        syncHardwareEffectsToCurrentPlayer()
    }

    fun updateEqBand(index: Short, levelMb: Short) {
        val clamped = levelMb.coerceIn(_state.value.eqMinMb, _state.value.eqMaxMb)
        val updated = _state.value.eqBands.map {
            if (it.index == index) it.copy(levelMb = clamped) else it
        }
        _state.update { it.copy(eqPreset = EqPreset.CUSTOM, eqBands = updated, eqEnabled = true) }
        syncHardwareEffectsToCurrentPlayer()
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { it.copy(bassBoostStrength = clamped, eqEnabled = true) }
        syncHardwareEffectsToCurrentPlayer()
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.update { it.copy(virtualizerStrength = clamped, eqEnabled = true) }
        syncHardwareEffectsToCurrentPlayer()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackSpeed = clamped) }
        applyPlaybackParams(activePlayer, clamped, _state.value.playbackPitch)
    }

    fun setPlaybackPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.6f, 1.5f)
        _state.update { it.copy(playbackPitch = clamped) }
        applyPlaybackParams(activePlayer, _state.value.playbackSpeed, clamped)
    }

    fun setPlayerVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.update { it.copy(playerVolume = clamped, isMuted = clamped == 0f) }
        applyEffectiveVolume(activePlayer)
    }

    fun toggleMute() {
        _state.update { current ->
            val nextMuted = !current.isMuted
            current.copy(isMuted = nextMuted)
        }
        applyEffectiveVolume(activePlayer)
    }

    fun toggleAbLoopPoint() {
        val current = _state.value
        val dur = current.durationMs.coerceAtLeast(1000L)
        val pos = current.positionMs.coerceIn(0L, dur)
        when {
            current.abLoopStartMs == null -> {
                _state.update { it.copy(abLoopStartMs = pos, abLoopEndMs = null) }
            }
            current.abLoopEndMs == null -> {
                val start = current.abLoopStartMs
                val end = if (pos > start + 1000L) pos else (start + 5000L).coerceAtMost(dur)
                _state.update { it.copy(abLoopEndMs = end) }
                seekTo(start)
            }
            else -> {
                _state.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
            }
        }
    }

    fun clearAbLoop() {
        _state.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _state.update { it.copy(sleepTimerRemainingMs = null, sleepAtTrackEnd = false) }
            return
        }
        val totalMs = minutes * 60_000L
        _state.update { it.copy(sleepTimerRemainingMs = totalMs, sleepAtTrackEnd = false) }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _state.update { it.copy(sleepTimerRemainingMs = if (remaining > 0L) remaining else null) }
                if (remaining in 1L..3000L) {
                    val factor = remaining / 3000f
                    val baseVol = effectiveVolume()
                    runCatching { activePlayer?.volume = baseVol * factor }
                }
            }
            if (isActive) {
                runCatching {
                    if (activePlayer?.isPlaying == true) activePlayer?.pause()
                }
                applyEffectiveVolume(activePlayer)
                _state.update { it.copy(isPlaying = false, sleepTimerRemainingMs = null) }
            }
        }
    }

    fun setSleepAtEndOfTrack() {
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerRemainingMs = null, sleepAtTrackEnd = true) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerRemainingMs = null, sleepAtTrackEnd = false) }
        applyEffectiveVolume(activePlayer)
    }

    fun removeFromQueue(index: Int) {
        _state.update { current ->
            if (index !in current.queue.indices || current.queue.size <= 1) return@update current
            if (index == current.currentIndex) return@update current
            val mutable = current.queue.toMutableList()
            mutable.removeAt(index)
            val adjustedIdx = if (index < current.currentIndex) current.currentIndex - 1 else current.currentIndex
            current.copy(queue = mutable, currentIndex = adjustedIdx.coerceIn(0, mutable.lastIndex))
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            if (fromIndex !in current.queue.indices || toIndex !in current.queue.indices || fromIndex == toIndex) {
                return@update current
            }
            val mutable = current.queue.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            val curIdx = when (current.currentIndex) {
                fromIndex -> toIndex
                in (minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) -> {
                    if (fromIndex < toIndex) current.currentIndex - 1 else current.currentIndex + 1
                }
                else -> current.currentIndex
            }
            current.copy(queue = mutable, currentIndex = curIdx)
        }
    }

    fun clearUpcomingQueue() {
        _state.update { current ->
            val cur = current.currentTrack ?: return@update current
            current.copy(queue = listOf(cur), currentIndex = 0)
        }
    }

    fun shuffleUpcomingQueue() {
        _state.update { current ->
            if (current.queue.size <= 2 || current.currentIndex !in current.queue.indices) return@update current
            val head = current.queue.take(current.currentIndex + 1)
            val tail = current.queue.drop(current.currentIndex + 1).shuffled()
            current.copy(queue = head + tail)
        }
    }

    fun resetAudioEffects() {
        val flatBands = _state.value.eqBands.map { it.copy(levelMb = 0) }
        _state.update {
            it.copy(
                eqEnabled = true,
                eqPreset = EqPreset.FLAT,
                eqBands = flatBands,
                bassBoostStrength = 0,
                virtualizerStrength = 0,
                playbackSpeed = 1.0f,
                playbackPitch = 1.0f,
                crossfadeSeconds = 3
            )
        }
        syncHardwareEffectsToCurrentPlayer()
        applyPlaybackParams(activePlayer, 1.0f, 1.0f)
    }

    fun replaceTrackInQueue(oldTrackId: String, updatedTrack: TrackItem) {
        _state.update { current ->
            val updatedQueue = current.queue.map {
                if (it.trackId == oldTrackId) updatedTrack else it
            }
            val updatedCurrent = if (current.currentTrack?.trackId == oldTrackId) updatedTrack else current.currentTrack
            current.copy(queue = updatedQueue, currentTrack = updatedCurrent, playbackError = null)
        }
        if (_state.value.currentTrack?.trackId == updatedTrack.trackId) {
            startTrackInternal(updatedTrack, crossfade = false)
        }
    }

    fun clearError() {
        _state.update { it.copy(playbackError = null) }
    }

    private fun startTrackInternal(track: TrackItem, crossfade: Boolean) {
        fadeInJob?.cancel()
        crossfadeTriggeredForTrackId = null
        startedNotifiedTrackId = null

        if (track.streamUrl.isBlank()) {
            _state.update {
                it.copy(
                    currentTrack = track,
                    isPlaying = false,
                    isBuffering = false,
                    playbackError = "No stream URL for ${track.title}. Try Smart Replace."
                )
            }
            return
        }

        val previousPlayer = activePlayer
        if (crossfade && previousPlayer != null && previousPlayer.isPlaying) {
            releaseFadingPlayer()
            fadingOutPlayer = previousPlayer
            fadeOutPlayerAsync(previousPlayer, _state.value.crossfadeSeconds)
        } else {
            releaseAudioEffects()
            releasePlayer(previousPlayer)
        }
        activePlayer = null

        _state.update {
            it.copy(
                currentTrack = track,
                isBuffering = true,
                isPlaying = false,
                positionMs = 0L,
                durationMs = track.durationMs.coerceAtLeast(1000L),
                abLoopStartMs = null,
                abLoopEndMs = null,
                playbackError = null
            )
        }

        scope.launch {
            runCatching {
                val player = ExoPlayer.Builder(appContext)
                    .setLooper(Looper.getMainLooper())
                    .setMediaSourceFactory(DefaultMediaSourceFactory(mediaDataSourceFactory))
                    .setAudioAttributes(mediaAudioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .build()

                activePlayer = player
                player.volume = if (crossfade && _state.value.crossfadeSeconds > 0) 0.05f else effectiveVolume()

                val snap = _state.value
                if (abs(snap.playbackSpeed - 1.0f) > 0.01f || abs(snap.playbackPitch - 1.0f) > 0.01f) {
                    player.playbackParameters = PlaybackParameters(snap.playbackSpeed, snap.playbackPitch)
                }

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (activePlayer !== player) return
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                _state.update { it.copy(isBuffering = true) }
                            }
                            Player.STATE_READY -> {
                                val dur = player.duration
                                    .takeIf { it != C.TIME_UNSET && it > 0L }
                                    ?: track.durationMs.coerceAtLeast(1000L)
                                syncHardwareEffectsToCurrentPlayer()
                                _state.update {
                                    it.copy(
                                        isBuffering = false,
                                        isPlaying = player.isPlaying,
                                        durationMs = dur,
                                        playbackError = null
                                    )
                                }
                                if (startedNotifiedTrackId != track.trackId) {
                                    startedNotifiedTrackId = track.trackId
                                    onTrackStarted?.invoke(track)
                                }
                                startProgressLoop()
                            }
                            Player.STATE_ENDED -> {
                                if (crossfadeTriggeredForTrackId != track.trackId) {
                                    if (_state.value.sleepAtTrackEnd) {
                                        _state.update {
                                            it.copy(isPlaying = false, positionMs = 0L, sleepAtTrackEnd = false)
                                        }
                                    } else {
                                        skipNext(fromAutoComplete = true)
                                    }
                                }
                            }
                            Player.STATE_IDLE -> Unit
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (activePlayer !== player) return
                        _state.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startProgressLoop()
                        }
                    }

                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (activePlayer !== player) return
                        syncHardwareEffectsToCurrentPlayer()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer error (${error.errorCodeName}) for ${track.title}", error)
                        if (activePlayer === player) {
                            _state.update {
                                it.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    playbackError = "Stream unavailable (${track.title}). Tap Smart Replace to heal."
                                )
                            }
                        }
                    }
                })

                val uri = Uri.parse(track.streamUrl)
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.play()
                warnIfDeviceMediaVolumeMuted()

                if (crossfade && _state.value.crossfadeSeconds > 0) {
                    fadeInPlayerAsync(player, _state.value.crossfadeSeconds)
                }
            }.onFailure { err ->
                Log.e(TAG, "Failed to initialize ExoPlayer for ${track.title}", err)
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        playbackError = err.localizedMessage ?: "Unable to play ${track.title}"
                    )
                }
            }
        }
    }

    private fun warnIfDeviceMediaVolumeMuted() {
        val am = audioManager ?: return
        val currentVol = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1)
        if (currentVol == 0 && !mutedVolumeWarningShown) {
            mutedVolumeWarningShown = true
            onPlaybackWarning?.invoke("Device media volume is muted (0%). Press Volume Up to hear sound.")
        }
    }

    /**
     * Attaches Android hardware AudioEffects strictly on-demand:
     * when EQ is flat/disabled and BassBoost/Virtualizer are 0, hardware effects
     * are released completely so AudioFlinger outputs clean, unattenuated PCM audio.
     */
    private fun syncHardwareEffectsToCurrentPlayer() {
        val player = activePlayer ?: return
        val sessionId = runCatching { player.audioSessionId }.getOrDefault(C.AUDIO_SESSION_ID_UNSET)
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return

        val snap = _state.value
        val needsEq = snap.eqEnabled && snap.eqBands.any { it.levelMb.toInt() != 0 }
        if (!needsEq) {
            runCatching {
                equalizer?.enabled = false
                equalizer?.release()
            }
            equalizer = null
        } else {
            runCatching {
                val eq = equalizer ?: Equalizer(0, sessionId).also { equalizer = it }
                val bands = eq.numberOfBands
                val range = eq.bandLevelRange
                val minMb = range.getOrElse(0) { -1500 }
                val maxMb = range.getOrElse(1) { 1500 }
                val currentLevels = snap.eqBands
                val resolvedBands = (0 until bands).map { idx ->
                    val bandIdx = idx.toShort()
                    val freqHz = (eq.getCenterFreq(bandIdx) / 1000).coerceAtLeast(20)
                    val targetLevel = currentLevels.getOrNull(idx)?.levelMb?.coerceIn(minMb, maxMb) ?: 0
                    eq.setBandLevel(bandIdx, targetLevel)
                    EqBandInfo(bandIdx, freqHz, targetLevel)
                }
                eq.enabled = true
                _state.update {
                    it.copy(
                        eqBands = resolvedBands,
                        eqMinMb = minMb,
                        eqMaxMb = maxMb
                    )
                }
            }.onFailure {
                Log.w(TAG, "Hardware Equalizer unavailable on this device: ${it.message}")
            }
        }

        val needsBass = snap.eqEnabled && snap.bassBoostStrength > 0
        if (!needsBass) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
        } else {
            runCatching {
                val bb = bassBoost ?: BassBoost(0, sessionId).also { bassBoost = it }
                if (bb.strengthSupported) {
                    bb.setStrength(snap.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
                bb.enabled = true
            }.onFailure {
                Log.w(TAG, "Hardware BassBoost unavailable on this device: ${it.message}")
            }
        }

        val needsVirt = snap.eqEnabled && snap.virtualizerStrength > 0
        if (!needsVirt) {
            runCatching {
                virtualizer?.enabled = false
                virtualizer?.release()
            }
            virtualizer = null
        } else {
            runCatching {
                val virt = virtualizer ?: Virtualizer(0, sessionId).also { virtualizer = it }
                if (virt.strengthSupported) {
                    virt.setStrength(snap.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
                virt.enabled = true
            }.onFailure {
                Log.w(TAG, "Hardware Virtualizer unavailable on this device: ${it.message}")
            }
        }
    }

    private fun effectiveVolume(): Float {
        val current = _state.value
        return if (current.isMuted) 0f else current.playerVolume.coerceIn(0f, 1f)
    }

    private fun applyEffectiveVolume(player: ExoPlayer?) {
        if (player == null) return
        val vol = effectiveVolume()
        runCatching { player.volume = vol }
    }

    private fun applyPlaybackParams(player: ExoPlayer?, speed: Float, pitch: Float) {
        if (player == null) return
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        val safePitch = pitch.coerceIn(0.6f, 1.5f)
        runCatching {
            player.playbackParameters = PlaybackParameters(safeSpeed, safePitch)
        }.onFailure {
            Log.w(TAG, "PlaybackParameters unavailable: ${it.message}")
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = activePlayer ?: break
                val isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
                if (isPlaying) {
                    val pos = runCatching { player.currentPosition }.getOrDefault(0L).coerceAtLeast(0L)
                    val rawDur = runCatching { player.duration }.getOrDefault(C.TIME_UNSET)
                    val dur = if (rawDur != C.TIME_UNSET && rawDur > 0L) rawDur else _state.value.durationMs
                    val snap = _state.value
                    val loopA = snap.abLoopStartMs
                    val loopB = snap.abLoopEndMs
                    if (loopA != null && loopB != null && pos >= loopB) {
                        seekTo(loopA)
                    } else {
                        _state.update {
                            it.copy(
                                positionMs = pos,
                                durationMs = dur.coerceAtLeast(1000L)
                            )
                        }
                    }

                    val cfMs = _state.value.crossfadeSeconds * 1000L
                    val currentTrack = _state.value.currentTrack
                    if (cfMs > 0 &&
                        !_state.value.sleepAtTrackEnd &&
                        _state.value.abLoopEndMs == null &&
                        currentTrack != null &&
                        _state.value.repeatMode != RepeatMode.ONE &&
                        _state.value.queue.size > 1 &&
                        dur > cfMs + 2500L &&
                        (dur - pos) <= cfMs &&
                        crossfadeTriggeredForTrackId != currentTrack.trackId
                    ) {
                        crossfadeTriggeredForTrackId = currentTrack.trackId
                        skipNext(fromAutoComplete = true)
                    }
                }
                delay(250L)
            }
        }
    }

    private fun fadeOutPlayerAsync(player: ExoPlayer, seconds: Int) {
        fadeOutJob?.cancel()
        fadeOutJob = scope.launch {
            val steps = (seconds * 10).coerceAtLeast(5)
            val startVol = runCatching { player.volume }.getOrDefault(effectiveVolume())
            for (step in steps downTo 0) {
                val vol = startVol * (step.toFloat() / steps.toFloat())
                runCatching { player.volume = vol }
                delay(100L)
            }
            releasePlayer(player)
            if (fadingOutPlayer === player) fadingOutPlayer = null
        }
    }

    private fun fadeInPlayerAsync(player: ExoPlayer, seconds: Int) {
        fadeInJob?.cancel()
        fadeInJob = scope.launch {
            val steps = (seconds * 10).coerceAtLeast(5)
            for (step in 1..steps) {
                if (activePlayer !== player) break
                val vol = (step.toFloat() / steps.toFloat()) * effectiveVolume()
                runCatching { player.volume = vol }
                delay(100L)
            }
            if (activePlayer === player) {
                applyEffectiveVolume(player)
            }
        }
    }

    private fun releaseAudioEffects() {
        runCatching {
            equalizer?.enabled = false
            equalizer?.release()
        }
        equalizer = null
        runCatching {
            bassBoost?.enabled = false
            bassBoost?.release()
        }
        bassBoost = null
        runCatching {
            virtualizer?.enabled = false
            virtualizer?.release()
        }
        virtualizer = null
    }

    private fun releaseFadingPlayer() {
        fadeOutJob?.cancel()
        releasePlayer(fadingOutPlayer)
        fadingOutPlayer = null
    }

    private fun releasePlayer(player: ExoPlayer?) {
        if (player == null) return
        runCatching {
            player.stop()
            player.release()
        }
    }

    fun release() {
        fadeInJob?.cancel()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        releaseAudioEffects()
        releaseFadingPlayer()
        releasePlayer(activePlayer)
        activePlayer = null
    }

    private companion object {
        const val TAG = "BloomeeAudioEngine"
    }
}
