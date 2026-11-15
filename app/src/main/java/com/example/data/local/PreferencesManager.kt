package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bloomeeDataStore by preferencesDataStore(name = "bloomeetunes_prefs")

data class UserPreferences(
    val darkTheme: Boolean = true,
    val dynamicArtworkAccent: Boolean = true,
    val crossfadeSeconds: Int = 3,
    val lyricsOffsetMs: Long = 0L,
    val eqEnabled: Boolean = true,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqCustomBandsMb: List<Short> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val playbackSpeed: Float = 1.0f
)

class PreferencesManager(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val dynamicAccentKey = booleanPreferencesKey("dynamic_artwork_accent")
    private val crossfadeKey = intPreferencesKey("crossfade_seconds")
    private val lyricsOffsetKey = longPreferencesKey("lyrics_offset_ms")
    private val eqEnabledKey = booleanPreferencesKey("eq_enabled")
    private val eqPresetKey = stringPreferencesKey("eq_preset")
    private val eqCustomBandsKey = stringPreferencesKey("eq_custom_bands_mb")
    private val bassBoostKey = intPreferencesKey("bass_boost_strength")
    private val virtualizerKey = intPreferencesKey("virtualizer_strength")
    private val playbackSpeedKey = floatPreferencesKey("playback_speed")

    val preferences: Flow<UserPreferences> = context.bloomeeDataStore.data.map { prefs ->
        val presetName = prefs[eqPresetKey] ?: EqPreset.FLAT.name
        val preset = EqPreset.entries.firstOrNull { it.name == presetName } ?: EqPreset.FLAT
        val rawBands = prefs[eqCustomBandsKey].orEmpty()
        val parsedBands = rawBands.split(",")
            .mapNotNull { it.trim().toShortOrNull() }
            .takeIf { it.isNotEmpty() }
            ?: listOf<Short>(0, 0, 0, 0, 0)
        UserPreferences(
            darkTheme = prefs[darkThemeKey] ?: true,
            dynamicArtworkAccent = prefs[dynamicAccentKey] ?: true,
            crossfadeSeconds = prefs[crossfadeKey] ?: 3,
            lyricsOffsetMs = prefs[lyricsOffsetKey] ?: 0L,
            eqEnabled = prefs[eqEnabledKey] ?: true,
            eqPreset = preset,
            eqCustomBandsMb = parsedBands,
            bassBoostStrength = (prefs[bassBoostKey] ?: 0).coerceIn(0, 1000),
            virtualizerStrength = (prefs[virtualizerKey] ?: 0).coerceIn(0, 1000),
            playbackSpeed = (prefs[playbackSpeedKey] ?: 1.0f).coerceIn(0.5f, 2.0f)
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.bloomeeDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setDynamicAccent(enabled: Boolean) {
        context.bloomeeDataStore.edit { it[dynamicAccentKey] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.bloomeeDataStore.edit { it[crossfadeKey] = seconds.coerceIn(0, 8) }
    }

    suspend fun setLyricsOffsetMs(offsetMs: Long) {
        context.bloomeeDataStore.edit { it[lyricsOffsetKey] = offsetMs.coerceIn(-5000L, 5000L) }
    }

    suspend fun setEqSettings(
        enabled: Boolean,
        preset: EqPreset,
        bandsMb: List<Short>? = null
    ) {
        context.bloomeeDataStore.edit {
            it[eqEnabledKey] = enabled
            it[eqPresetKey] = preset.name
            if (bandsMb != null && bandsMb.isNotEmpty()) {
                it[eqCustomBandsKey] = bandsMb.joinToString(",")
            }
        }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.bloomeeDataStore.edit { it[bassBoostKey] = strength.coerceIn(0, 1000) }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.bloomeeDataStore.edit { it[virtualizerKey] = strength.coerceIn(0, 1000) }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.bloomeeDataStore.edit { it[playbackSpeedKey] = speed.coerceIn(0.5f, 2.0f) }
    }
}
