package dev.thomas_kiljanczyk.openpiano.core.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.thomas_kiljanczyk.openpiano.core.common.allowingThreadDiskWrites

/** Applies [orientation] for as long as the caller is composed, then restores the previous one. */
@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, orientation) {
        val previous = activity.requestedOrientation
        // setRequestedOrientation triggers a disk write in system_server, relayed here by StrictMode.
        allowingThreadDiskWrites { activity.requestedOrientation = orientation }
        onDispose { allowingThreadDiskWrites { activity.requestedOrientation = previous } }
    }
}
