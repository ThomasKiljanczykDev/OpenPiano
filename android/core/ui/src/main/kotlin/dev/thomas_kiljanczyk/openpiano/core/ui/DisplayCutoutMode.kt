package dev.thomas_kiljanczyk.openpiano.core.ui

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/** Applies [mode] (a `WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_*` constant) for
 * as long as the caller is composed, then restores the previous mode. No-op below API 28, where
 * the field does not exist. */
@Composable
fun LockDisplayCutoutMode(mode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return
    }
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, mode) {
        val window = activity.window
        val previous = window.attributes.layoutInDisplayCutoutMode
        window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = mode }
        onDispose {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = previous }
        }
    }
}
