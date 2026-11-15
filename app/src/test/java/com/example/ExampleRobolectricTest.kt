package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.audio.BloomeeAudioEngine
import com.example.audio.EqPreset
import com.example.audio.PlayerUiState
import com.example.data.model.TrackItem
import com.example.data.repository.IndianSingerProfile
import com.example.data.repository.MusicCatalogRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CollabHubScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FullPlayerContent
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.BloomeeTunesTheme
import com.example.ui.viewmodel.HomeAndExploreState
import com.example.ui.viewmodel.LyricsUiState
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BloomeeTunes", appName)
  }

  @Test
  fun `parseAudiusTrackList parses full length duration and stream url`() {
    val json = """
      {
        "data": [
          {
            "id": "x912k",
            "title": "Midnight Horizon",
            "duration": 224,
            "is_streamable": true,
            "genre": "Electronic",
            "mood": "Dreamy",
            "user": { "name": "Aurora Pulse" },
            "artwork": { "480x480": "https://cdn.audius.co/art480.jpg" }
          },
          {
            "id": "blocked1",
            "title": "Unstreamable Track",
            "duration": 180,
            "is_streamable": false,
            "user": { "name": "Someone" }
          }
        ]
      }
    """.trimIndent()
    val tracks = MusicCatalogRepository.parseAudiusTrackList(json, "https://discovery.audius.co")
    assertEquals(1, tracks.size)
    assertEquals("audius_x912k", tracks[0].trackId)
    assertEquals("Midnight Horizon", tracks[0].title)
    assertEquals("Aurora Pulse", tracks[0].artist)
    assertEquals(224_000L, tracks[0].durationMs)
    assertEquals("https://discovery.audius.co/v1/tracks/x912k/stream?app_name=BloomeeTunes", tracks[0].streamUrl)
  }

  @Test
  fun `equalizerScreen displays frequency sliders and applies preset changes`() {
    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        var state by remember { mutableStateOf(PlayerUiState()) }
        EqualizerScreen(
          playerState = state,
          onBack = {},
          onTogglePlayPause = {},
          onToggleEqEnabled = { state = state.copy(eqEnabled = it) },
          onApplyEqPreset = { preset ->
            val updated = state.eqBands.mapIndexed { idx, b ->
              b.copy(levelMb = preset.gainsMb.getOrElse(idx) { 0 })
            }
            state = state.copy(eqPreset = preset, eqBands = updated, eqEnabled = true)
          },
          onUpdateEqBand = { index, levelMb ->
            val updated = state.eqBands.map {
              if (it.index == index) it.copy(levelMb = levelMb) else it
            }
            state = state.copy(eqPreset = EqPreset.CUSTOM, eqBands = updated)
          },
          onBassBoostChange = { state = state.copy(bassBoostStrength = it) },
          onVirtualizerChange = { state = state.copy(virtualizerStrength = it) },
          onPlaybackSpeedChange = { state = state.copy(playbackSpeed = it) },
          onCrossfadeChange = { state = state.copy(crossfadeSeconds = it) },
          onResetEffects = { state = PlayerUiState() }
        )
      }
    }

    composeTestRule.onNodeWithText("Audio Equalizer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_master_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_presets_row").performScrollTo()
    composeTestRule.onNodeWithTag("eq_preset_bass_boost").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Active • Bass Bloom").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_band_slider_0").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_band_slider_4").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_bass_boost_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_speed_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_reset_button").performClick()
    composeTestRule.onNodeWithText("Active • Balanced").assertIsDisplayed()
  }

  @Test
  fun `authScreen displays Continue as Guest button and triggers callback`() {
    var enteredGuest = false
    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        AuthScreen(
          onAuthSuccess = {},
          onContinueAsGuest = { enteredGuest = true }
        )
      }
    }
    composeTestRule.onNodeWithTag("guest_mode_button").performScrollTo().performClick()
    assertTrue(enteredGuest)
  }

  @Test
  fun `collabHubScreen in guest mode prompts Google Sign-In`() {
    var requestedSignIn = false
    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        CollabHubScreen(
          playlistsState = UiState.Success(emptyList()),
          isGuest = true,
          onOpenPlaylist = {},
          onShowCreateDialog = {},
          onShowJoinDialog = {},
          onRequestSignIn = { requestedSignIn = true }
        )
      }
    }
    composeTestRule.onNodeWithTag("collab_guest_sign_in_button").performScrollTo().performClick()
    assertTrue(requestedSignIn)
  }

  @Test
  fun `homeScreen showcases Indian and Bollywood singers spotlight`() {
    var selectedSinger: IndianSingerProfile? = null
    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        HomeScreen(
          homeExplore = HomeAndExploreState(chartsLoading = false),
          likedTracksState = UiState.Success(emptyList()),
          historyState = UiState.Success(emptyList()),
          playlistsState = UiState.Success(emptyList()),
          isGuest = true,
          currentTrackId = null,
          onPlayTrack = { _, _ -> },
          onPlayQueue = {},
          onToggleLike = { _, _ -> },
          onAddToPlaylist = {},
          onAddToQueue = {},
          onSmartReplace = {},
          onOpenPlaylist = {},
          onNavigateToCollab = {},
          onOpenEqualizer = {},
          onSelectSinger = { selectedSinger = it },
          onRetryCharts = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Indian & Bollywood Singers").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_singers_row").performScrollTo()
    composeTestRule.onNodeWithTag("home_singer_card_arijit").performScrollTo().performClick()
    assertEquals("Arijit Singh", selectedSinger?.name)
  }

  @Test
  fun `exploreScreen shows Bollywood genres and Indian singer chips`() {
    var selectedSinger: IndianSingerProfile? = null
    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        ExploreScreen(
          homeExplore = HomeAndExploreState(chartsLoading = false),
          likedIds = emptySet(),
          currentTrackId = null,
          onSearchQueryChange = {},
          onExecuteSearch = {},
          onSelectGenre = {},
          onSelectSinger = { selectedSinger = it },
          onPlayTrack = { _, _ -> },
          onToggleLike = { _, _ -> },
          onAddToPlaylist = {},
          onAddToQueue = {},
          onSmartReplace = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Bollywood & Indian Music").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bollywood Hits").assertIsDisplayed()
    composeTestRule.onNodeWithTag("explore_singers_row").performScrollTo()
    composeTestRule.onNodeWithTag("explore_singer_chip_shreya").performScrollTo().performClick()
    assertEquals("Shreya Ghoshal", selectedSinger?.name)
  }

  @Test
  fun `fullPlayerContent exposes advanced audio tools and interactive dialogs`() {
    val sampleTrack = TrackItem(
      trackId = "bolly_1",
      title = "Kesariya",
      artist = "Arijit Singh",
      album = "Brahmastra",
      durationMs = 268_000L
    )
    var seekDelta = 0L
    var selectedSpeed = 1.0f
    var sleepMinutes = 0
    var abLoopToggles = 0
    var openedEqStudio = false

    composeTestRule.setContent {
      BloomeeTunesTheme(darkTheme = true) {
        var activeTab by remember { mutableStateOf(0) }
        var state by remember {
          mutableStateOf(
            PlayerUiState(
              currentTrack = sampleTrack,
              isPlaying = true,
              positionMs = 45_000L,
              durationMs = 268_000L
            )
          )
        }
        FullPlayerContent(
          track = sampleTrack,
          playerState = state,
          lyricsState = LyricsUiState(),
          lyricsOffsetMs = 0L,
          isLiked = false,
          activeTab = activeTab,
          onTabChange = { activeTab = it },
          onDismiss = {},
          onTogglePlayPause = { state = state.copy(isPlaying = !state.isPlaying) },
          onSeekTo = { state = state.copy(positionMs = it) },
          onSkipNext = {},
          onSkipPrevious = {},
          onToggleShuffle = { state = state.copy(shuffleEnabled = !state.shuffleEnabled) },
          onCycleRepeat = {},
          onToggleLike = { _, _ -> },
          onAddToPlaylist = {},
          onSmartReplace = {},
          onPlayQueueItem = {},
          onAdjustLyricsOffset = {},
          onResetLyricsOffset = {},
          onToggleEqEnabled = {},
          onApplyEqPreset = {},
          onUpdateEqBand = { _, _ -> },
          onBassBoostChange = {},
          onVirtualizerChange = {},
          onPlaybackSpeedChange = {
            selectedSpeed = it
            state = state.copy(playbackSpeed = it)
          },
          onSetCrossfadeSeconds = {},
          onOpenFullEqualizerScreen = { openedEqStudio = true },
          onSeekBy = { seekDelta = it },
          onToggleAbLoop = {
            abLoopToggles += 1
            state = state.copy(abLoopStartMs = 45_000L)
          },
          onSetSleepTimerMinutes = { mins ->
            sleepMinutes = mins
            state = state.copy(sleepTimerRemainingMs = mins * 60_000L)
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("player_play_pause_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollTo()
    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_replay_10_button"))
    composeTestRule.onNodeWithTag("player_replay_10_button").performClick()
    assertEquals(-10_000L, seekDelta)

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_forward_10_button"))
    composeTestRule.onNodeWithTag("player_forward_10_button").performClick()
    assertEquals(10_000L, seekDelta)

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_ab_loop_button"))
    composeTestRule.onNodeWithTag("player_ab_loop_button").performClick()
    assertEquals(1, abLoopToggles)

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_speed_button"))
    composeTestRule.onNodeWithTag("player_speed_button").performClick()
    composeTestRule.onNodeWithText("Speed & Pitch Studio").assertIsDisplayed()
    composeTestRule.onNodeWithText("1.25x").performClick()
    assertEquals(1.25f, selectedSpeed, 0.01f)
    composeTestRule.onNodeWithText("Done").performClick()

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_sleep_timer_button"))
    composeTestRule.onNodeWithTag("player_sleep_timer_button").performClick()
    composeTestRule.onNodeWithText("Sleep Timer").assertIsDisplayed()
    composeTestRule.onNodeWithText("15 min").performClick()
    assertEquals(15, sleepMinutes)
    composeTestRule.onNodeWithText("Sleep 15:00").assertIsDisplayed()

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_volume_button"))
    composeTestRule.onNodeWithTag("player_volume_button").performClick()
    composeTestRule.onNodeWithText("Player Gain & Mute").assertIsDisplayed()
    composeTestRule.onNodeWithText("Done").performClick()

    composeTestRule.onNodeWithTag("player_advanced_tools_dock").performScrollToNode(hasTestTag("player_eq_studio_button"))
    composeTestRule.onNodeWithTag("player_eq_studio_button").performClick()
    assertTrue(openedEqStudio)
  }

  @Test
  fun `bloomeeAudioEngine manages volume mute speed pitch and flat EQ cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = BloomeeAudioEngine(context)
    try {
      assertEquals(1.0f, engine.state.value.playerVolume, 0.01f)
      assertEquals(false, engine.state.value.isMuted)

      engine.setPlayerVolume(0.65f)
      assertEquals(0.65f, engine.state.value.playerVolume, 0.01f)
      assertEquals(false, engine.state.value.isMuted)

      engine.toggleMute()
      assertTrue(engine.state.value.isMuted)
      engine.toggleMute()
      assertEquals(false, engine.state.value.isMuted)

      engine.setPlaybackSpeed(1.25f)
      engine.setPlaybackPitch(0.95f)
      assertEquals(1.25f, engine.state.value.playbackSpeed, 0.01f)
      assertEquals(0.95f, engine.state.value.playbackPitch, 0.01f)

      engine.applyEqPreset(EqPreset.BASS_BOOST)
      assertEquals(EqPreset.BASS_BOOST, engine.state.value.eqPreset)
      engine.resetAudioEffects()
      assertEquals(EqPreset.FLAT, engine.state.value.eqPreset)
      assertEquals(1.0f, engine.state.value.playbackSpeed, 0.01f)
      assertEquals(1.0f, engine.state.value.playbackPitch, 0.01f)
    } finally {
      engine.release()
    }
  }
}
