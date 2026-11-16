package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firestore.KlydoRepository
import com.example.data.local.GuestKlydoRepository
import com.example.ui.KlydoDashboardRoute
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.theme.KlydoDayTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    private val sharedVideoUriFlow = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighRefreshRateModeIfSupported()
        handleIncomingShareIntent(intent)
        setContent {
            val sharedVideoUri by sharedVideoUriFlow.collectAsStateWithLifecycle()
            KlydoDayTheme {
                AppNavigation(
                    sharedVideoUri = sharedVideoUri,
                    onConsumeSharedVideo = { sharedVideoUriFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("video/") == true) {
            val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            if (uri != null) {
                sharedVideoUriFlow.value = uri
            }
        }
    }

    private fun requestHighRefreshRateModeIfSupported() {
        val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        val currentMode = activeDisplay.mode ?: return
        val highRefreshMode = activeDisplay.supportedModes
            .filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight &&
                    mode.refreshRate >= 89.5f
            }
            .minByOrNull { it.refreshRate }

        if (highRefreshMode != null && highRefreshMode.modeId != currentMode.modeId) {
            val attrs = window.attributes
            attrs.preferredDisplayModeId = highRefreshMode.modeId
            window.attributes = attrs
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    sharedVideoUri: Uri? = null,
    onConsumeSharedVideo: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Attempt silent Google restore in the background without blocking Guest Mode launch
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    // Migrate local guest schedule blocks to Firestore when a user signs in with Google
    LaunchedEffect(user?.uid) {
        if (user != null) {
            val guestRepo = GuestKlydoRepository(context.applicationContext)
            val guestBlocks = guestRepo.getGuestBlocksSnapshot()
            if (guestBlocks.isNotEmpty()) {
                val databaseId = context.getString(R.string.firestore_database_id)
                val cloudRepo = KlydoRepository(FirebaseFirestore.getInstance(databaseId), auth)
                var allMigrated = true
                for (block in guestBlocks) {
                    val result = cloudRepo.createScheduleBlock(block)
                    if (result.isFailure) {
                        allMigrated = false
                    }
                }
                if (allMigrated) {
                    guestRepo.clearGuestBlocks()
                }
            }
        }
    }

    if (user == null) {
        key(GuestKlydoRepository.GUEST_USER_ID) {
            KlydoDashboardRoute(
                sessionKey = GuestKlydoRepository.GUEST_USER_ID,
                isGuest = true,
                userEmail = null,
                sharedVideoUri = sharedVideoUri,
                onConsumeSharedVideo = onConsumeSharedVideo
            )
        }
    } else {
        key(user.uid) {
            KlydoDashboardRoute(
                sessionKey = user.uid,
                isGuest = false,
                userEmail = user.email,
                sharedVideoUri = sharedVideoUri,
                onConsumeSharedVideo = onConsumeSharedVideo
            )
        }
    }
}
