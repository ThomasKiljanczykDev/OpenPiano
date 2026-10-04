package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

enum class TourCardPosition {
    /** Below the anchor when there is room, otherwise above. */
    AUTO,

    TOP,

    BOTTOM,
}

object TourCardPlacement {

    /** If neither side fits, the card is clamped over the anchor: legible text beats an intact cutout. */
    fun offsetFor(
        anchor: Rect?,
        cardWidth: Float,
        cardHeight: Float,
        containerWidth: Float,
        containerHeight: Float,
        insetLeft: Float = 0f,
        insetTop: Float = 0f,
        insetRight: Float = 0f,
        insetBottom: Float = 0f,
        gap: Float = 0f,
        position: TourCardPosition = TourCardPosition.AUTO,
    ): Pair<Float, Float> {
        val minX = insetLeft
        val maxX = max(minX, containerWidth - insetRight - cardWidth)
        val minY = insetTop
        val maxY = max(minY, containerHeight - insetBottom - cardHeight)

        val centreX = anchor?.center?.x ?: (containerWidth / 2f)
        val x = clamp(centreX - cardWidth / 2f, minX, maxX)
        val centredY = (containerHeight - cardHeight) / 2f

        val y = when {
            position == TourCardPosition.TOP -> minY
            position == TourCardPosition.BOTTOM -> maxY
            anchor == null -> centredY
            anchor.bottom + gap + cardHeight <= containerHeight - insetBottom -> anchor.bottom + gap
            anchor.top - gap - cardHeight >= insetTop -> anchor.top - gap - cardHeight
            else -> centredY
        }
        return x to clamp(y, minY, maxY)
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float =
        min(max(value, minValue), maxValue)
}
