package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/** Empty [anchors] means a centred card with no cutout. */
@Immutable
data class TourStep(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val anchors: List<TourAnchor> = emptyList(),
    val cardPosition: TourCardPosition = TourCardPosition.AUTO,
    val onEnter: () -> Unit = {},
)
