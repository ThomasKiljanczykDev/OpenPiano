package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

enum class TourExpandable {
    PRECISION_SLIDER,
}

/** Owners OR this with their own visibility condition; never mutate the backing preference. */
@Stable
fun interface TourExpansion {
    fun isForcedOpen(expandable: TourExpandable): Boolean

    companion object {
        val None = TourExpansion { false }
    }
}

val LocalTourExpansion = compositionLocalOf { TourExpansion.None }
