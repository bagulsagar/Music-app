package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.EqBandInfo
import com.example.audio.EqPreset
import com.example.audio.PlayerUiState
import com.example.ui.components.AlbumArtImage
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BloomCoral
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SonicMint
import java.util.Locale

@Composable
fun EqualizerScreen(
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onUpdateEqBand: (Short, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onResetEffects: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("eq_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio Equalizer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (playerState.eqEnabled) {
                        "Active • ${playerState.eqPreset.label}"
                    } else {
                        "Bypassed • Flat output"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (playerState.eqEnabled) SonicMint else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onResetEffects,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("eq_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset equalizer",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Switch(
                checked = playerState.eqEnabled,
                onCheckedChange = onToggleEqEnabled,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("eq_master_switch")
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp)
        ) {
            // Active Track Status Card
            run {
                val track = playerState.currentTrack
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track != null) {
                            AlbumArtImage(
                                artworkUrl = track.artworkUrl,
                                contentDescription = "${track.title} cover art",
                                modifier = Modifier.size(46.dp),
                                cornerRadius = 12.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • Live DSP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SonicMint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = SonicMint
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Audio Studio Ready",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Settings save automatically and apply as soon as a song plays.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            EqualizerStudioContent(
                playerState = playerState,
                onToggleEqEnabled = onToggleEqEnabled,
                onApplyEqPreset = onApplyEqPreset,
                onUpdateEqBand = onUpdateEqBand,
                onBassBoostChange = onBassBoostChange,
                onVirtualizerChange = onVirtualizerChange,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                onCrossfadeChange = onCrossfadeChange,
                showHeaderToggle = false
            )
        }
    }
}

@Composable
fun EqualizerStudioContent(
    playerState: PlayerUiState,
    onToggleEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onUpdateEqBand: (Short, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    showHeaderToggle: Boolean = true,
    onOpenFullEqualizerScreen: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (showHeaderToggle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = SonicMint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5-Band Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenFullEqualizerScreen != null) {
                        TextButton(onClick = onOpenFullEqualizerScreen) {
                            Text("Full Studio")
                        }
                    }
                    Switch(
                        checked = playerState.eqEnabled,
                        onCheckedChange = onToggleEqEnabled
                    )
                }
            }
        }

        // Frequency Response Curve Visualizer
        FrequencyResponseCard(
            bands = playerState.eqBands,
            minMb = playerState.eqMinMb,
            maxMb = playerState.eqMaxMb,
            enabled = playerState.eqEnabled,
            presetLabel = playerState.eqPreset.label
        )

        // Presets Strip
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Sound Profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("eq_presets_row")
            ) {
                items(EqPreset.entries) { preset ->
                    val isSelected = playerState.eqPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (preset != EqPreset.CUSTOM) {
                                onApplyEqPreset(preset)
                            }
                        },
                        enabled = preset != EqPreset.CUSTOM || isSelected,
                        label = { Text(preset.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("eq_preset_${preset.name.lowercase()}")
                    )
                }
            }
        }

        // Frequency Band Sliders Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (playerState.eqEnabled) 1f else 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Frequency Bands",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Tap dB to zero",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val minDb = playerState.eqMinMb / 100
                val maxDb = playerState.eqMaxMb / 100

                playerState.eqBands.forEachIndexed { idx, band ->
                    val bandRole = bandRoleLabel(idx, band.centerFreqHz)
                    val bandColor = bandAccentColor(idx)
                    val dbText = formatDecibels(band.levelMb)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bandColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatFrequency(band.centerFreqHz),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bandRole,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = bandColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = playerState.eqEnabled) {
                                        onUpdateEqBand(band.index, 0)
                                    }
                            ) {
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = bandColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { onUpdateEqBand(band.index, it.toInt().toShort()) },
                            valueRange = playerState.eqMinMb.toFloat()..playerState.eqMaxMb.toFloat(),
                            enabled = playerState.eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = bandColor,
                                activeTrackColor = bandColor,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("eq_band_slider_${band.index}")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${minDb} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0 dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${maxDb} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (idx < playerState.eqBands.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }

        // Spatial & Playback Dynamics Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Playback & Spatial Effects",
                    style = MaterialTheme.typography.titleLarge
                )

                // Bass Boost Slider
                EffectSliderRow(
                    icon = Icons.Rounded.Speaker,
                    title = "Bass Boost",
                    valueLabel = "${playerState.bassBoostStrength / 10}%",
                    accent = BloomCoral,
                    value = playerState.bassBoostStrength.toFloat(),
                    valueRange = 0f..1000f,
                    enabled = playerState.eqEnabled,
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    testTag = "eq_bass_boost_slider"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Spatial Virtualizer Slider
                EffectSliderRow(
                    icon = Icons.Rounded.SurroundSound,
                    title = "3D Spatial Virtualizer",
                    valueLabel = "${playerState.virtualizerStrength / 10}%",
                    accent = SonicMint,
                    value = playerState.virtualizerStrength.toFloat(),
                    valueRange = 0f..1000f,
                    enabled = playerState.eqEnabled,
                    onValueChange = { onVirtualizerChange(it.toInt()) },
                    testTag = "eq_virtualizer_slider"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Playback Speed Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playback Tempo",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            color = AmberGlow.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onPlaybackSpeedChange(1.0f) }
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fx", playerState.playbackSpeed),
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGlow,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Slider(
                        value = playerState.playbackSpeed,
                        onValueChange = { raw ->
                            val stepped = (Math.round(raw * 20f) / 20f).coerceIn(0.5f, 2.0f)
                            onPlaybackSpeedChange(stepped)
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberGlow,
                            activeTrackColor = AmberGlow,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("eq_speed_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.5x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.0x Normal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2.0x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Track Crossfade Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track Crossfade",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (playerState.crossfadeSeconds == 0) "Off" else "${playerState.crossfadeSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricViolet
                        )
                    }
                    Slider(
                        value = playerState.crossfadeSeconds.toFloat(),
                        onValueChange = { onCrossfadeChange(it.toInt()) },
                        valueRange = 0f..8f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricViolet,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("eq_crossfade_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyResponseCard(
    bands: List<EqBandInfo>,
    minMb: Short,
    maxMb: Short,
    enabled: Boolean,
    presetLabel: String
) {
    val animatedLevels = bands.map { band ->
        val target = if (enabled) band.levelMb.toFloat() else 0f
        animateFloatAsState(
            targetValue = target,
            animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f),
            label = "eq_band_anim_${band.index}"
        ).value
    }

    val minVal = minMb.toFloat().coerceAtMost(-100f)
    val maxVal = maxMb.toFloat().coerceAtLeast(100f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (enabled) SonicMint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequency Response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = (if (enabled) SonicMint else MaterialTheme.colorScheme.outline).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (enabled) presetLabel else "Bypassed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) SonicMint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                val w = size.width
                val h = size.height
                val padX = 18.dp.toPx()
                val padY = 14.dp.toPx()
                val plotW = (w - padX * 2).coerceAtLeast(1f)
                val plotH = (h - padY * 2).coerceAtLeast(1f)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                // Top, center (0 dB), and bottom grid lines
                drawLine(
                    color = gridColor,
                    start = Offset(padX, padY),
                    end = Offset(w - padX, padY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                val zeroY = padY + plotH * ((maxVal - 0f) / (maxVal - minVal))
                drawLine(
                    color = zeroLineColor,
                    start = Offset(padX, zeroY),
                    end = Offset(w - padX, zeroY),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(padX, h - padY),
                    end = Offset(w - padX, h - padY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                if (animatedLevels.isNotEmpty()) {
                    val count = animatedLevels.size
                    val points = animatedLevels.mapIndexed { idx, level ->
                        val x = if (count == 1) w / 2f else padX + (plotW * idx / (count - 1).toFloat())
                        val norm = ((maxVal - level) / (maxVal - minVal)).coerceIn(0f, 1f)
                        val y = padY + plotH * norm
                        Offset(x, y)
                    }

                    val curvePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(curvePath)
                        lineTo(points.last().x, zeroY)
                        lineTo(points.first().x, zeroY)
                        close()
                    }

                    val gradientColors = if (enabled) {
                        listOf(BloomCoral, ElectricViolet, SonicMint)
                    } else {
                        listOf(gridColor, gridColor)
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.horizontalGradient(
                            colors = gradientColors.map { it.copy(alpha = if (enabled) 0.22f else 0.08f) }
                        )
                    )

                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(colors = gradientColors),
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { idx, pt ->
                        val dotColor = if (enabled) bandAccentColor(idx) else gridColor
                        drawCircle(
                            color = dotColor.copy(alpha = 0.28f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bands.forEach { band ->
                    Text(
                        text = formatFrequency(band.centerFreqHz),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectSliderRow(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    accent: Color,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

internal fun formatFrequency(freqHz: Int): String =
    if (freqHz >= 1000) {
        val khz = freqHz / 1000.0
        if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", khz)
    } else {
        "$freqHz Hz"
    }

internal fun formatDecibels(levelMb: Short): String {
    val db = levelMb / 100.0
    return if (db > 0.0) String.format(Locale.US, "+%.1f dB", db)
    else String.format(Locale.US, "%.1f dB", db)
}

private fun bandRoleLabel(index: Int, freqHz: Int): String = when {
    index == 0 || freqHz <= 100 -> "Sub-Bass"
    index == 1 || freqHz <= 400 -> "Bass & Warmth"
    index == 2 || freqHz <= 1500 -> "Vocals & Mids"
    index == 3 || freqHz <= 6000 -> "Presence"
    else -> "Brilliance"
}

private fun bandAccentColor(index: Int): Color = when (index % 5) {
    0 -> BloomCoral
    1 -> AmberGlow
    2 -> ElectricViolet
    3 -> SonicMint
    else -> Color(0xFF4DA6FF)
}
