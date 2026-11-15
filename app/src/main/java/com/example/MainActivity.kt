package com.example

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.authStateFlow
import com.example.ui.MainFeatureScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.BloomeeTunesTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private const val GUEST_SESSION_ID = "__guest_session__"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        setContent {
            BloomeeTunesTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by rememberSaveable { mutableStateOf(false) }
    val user = currentUser

    LaunchedEffect(user?.uid) {
        if (user != null) {
            isGuestMode = false
        }
    }

    when {
        user != null -> {
            key(user.uid) {
                MainFeatureScreen(
                    currentUserId = user.uid,
                    isGuest = false,
                    onRequestSignIn = {}
                )
            }
        }
        isGuestMode -> {
            key(GUEST_SESSION_ID) {
                MainFeatureScreen(
                    currentUserId = GUEST_SESSION_ID,
                    isGuest = true,
                    onRequestSignIn = { isGuestMode = false }
                )
            }
        }
        else -> {
            AuthScreen(
                onAuthSuccess = { isGuestMode = false },
                onContinueAsGuest = { isGuestMode = true }
            )
        }
    }
}
