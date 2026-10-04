package dev.thomas_kiljanczyk.openpiano.core.ui

import android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.thomas_kiljanczyk.openpiano.core.common.allowingThreadDiskWrites
import kotlinx.coroutines.flow.first

/** Applies [orientation] once the caller's lifecycle first resumes, until it leaves composition. */
@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // A predictive-back preview composes the entry at STARTED; rotating then cancels the gesture.
    var resumed by remember(lifecycle) { mutableStateOf(false) }
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        resumed = true
    }

    if (!resumed) {
        return
    }

    DisposableEffect(activity, orientation) {
        val window = activity.window
        val previous = activity.requestedOrientation
        val previousAnimation = window.attributes.rotationAnimation
        // LandscapeLayout pre-rotates content, so an animated rotation adds nothing.
        window.attributes = window.attributes.apply { rotationAnimation = ROTATION_ANIMATION_SEAMLESS }
        // setRequestedOrientation triggers a disk write in system_server, relayed here by StrictMode.
        allowingThreadDiskWrites { activity.requestedOrientation = orientation }
        onDispose {
            window.attributes = window.attributes.apply { rotationAnimation = previousAnimation }
            allowingThreadDiskWrites { activity.requestedOrientation = previous }
        }
    }
}
