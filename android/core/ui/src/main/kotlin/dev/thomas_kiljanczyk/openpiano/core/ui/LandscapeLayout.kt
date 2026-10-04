package dev.thomas_kiljanczyk.openpiano.core.ui

import android.content.res.Configuration
import android.view.Surface
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val COMPACT_SMALLEST_WIDTH_DP = 600
private const val QUARTER_TURN_DEGREES = 90f

/** Insets for [LandscapeLayout] content to use in place of the [WindowInsets] companions. */
@Stable
class LandscapeLayoutScope internal constructor(
    val safeDrawing: WindowInsets,
    val displayCutout: WindowInsets,
)

/**
 * Lays out [content] in landscape, rotated a quarter turn on a portrait phone window,
 * e.g. a predictive-back preview before a landscape lock applies.
 */
@Composable
fun LandscapeLayout(modifier: Modifier = Modifier, content: @Composable LandscapeLayoutScope.() -> Unit) {
    val configuration = LocalConfiguration.current
    // Large screens and multi-window may ignore the orientation lock and stay portrait.
    val rotate = configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
        configuration.smallestScreenWidthDp < COMPACT_SMALLEST_WIDTH_DP &&
        LocalActivity.current?.isInMultiWindowMode != true
    val safeDrawing = WindowInsets.safeDrawing
    val displayCutout = WindowInsets.displayCutout
    val density = LocalDensity.current
    val displayRotation = LocalView.current.display?.rotation
    // Kept while another destination covers this one, for a back preview to reuse.
    var landscapeInsets by rememberSaveable(configuration.smallestScreenWidthDp, configuration.densityDpi) {
        mutableStateOf<IntArray?>(null)
    }
    if (!rotate && displayRotation == Surface.ROTATION_90) {
        LaunchedEffect(safeDrawing, displayCutout, density) {
            snapshotFlow { safeDrawing.toIntArray(density) + displayCutout.toIntArray(density) }
                .collect { landscapeInsets = it }
        }
    }
    val saved = landscapeInsets
    val scope = when {
        !rotate -> LandscapeLayoutScope(safeDrawing = safeDrawing, displayCutout = displayCutout)

        saved != null -> LandscapeLayoutScope(
            safeDrawing = WindowInsets(saved[0], saved[1], saved[2], saved[3]),
            displayCutout = WindowInsets(saved[4], saved[5], saved[6], saved[7]),
        )

        else -> {
            // Portrait system bars sit where landscape has none, so content draws beneath them.
            val cutout = QuarterTurnedInsets(displayCutout)
            LandscapeLayoutScope(safeDrawing = cutout, displayCutout = cutout)
        }
    }
    Box(if (rotate) modifier.rotatedQuarterTurn() else modifier) { scope.content() }
}

private fun WindowInsets.toIntArray(density: Density): IntArray = intArrayOf(
    getLeft(density, LayoutDirection.Ltr),
    getTop(density),
    getRight(density, LayoutDirection.Ltr),
    getBottom(density),
)

/** [source] as seen by content turned a quarter clockwise, matching ROTATION_90. */
private class QuarterTurnedInsets(private val source: WindowInsets) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = source.getTop(density)

    override fun getTop(density: Density): Int = source.getRight(density, LayoutDirection.Ltr)

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = source.getBottom(density)

    override fun getBottom(density: Density): Int = source.getLeft(density, LayoutDirection.Ltr)

    override fun equals(other: Any?): Boolean = other is QuarterTurnedInsets && other.source == source

    override fun hashCode(): Int = source.hashCode()
}

private fun Modifier.rotatedQuarterTurn(): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(width = height, height = width))
    layout(width, height) {
        placeable.placeWithLayer(x = (width - height) / 2, y = (height - width) / 2) {
            rotationZ = QUARTER_TURN_DEGREES
        }
    }
}
