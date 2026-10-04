package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.bringIntoView

/** Screen space, not window space, so anchors in popup windows line up. No entry means cannot spotlight. */
@Stable
class TourAnchorRegistry {
    private val bounds = mutableStateMapOf<TourAnchor, Rect>()
    private val scrollers = mutableMapOf<TourAnchor, TourAnchorScroller>()

    operator fun get(anchor: TourAnchor): Rect? = bounds[anchor]

    fun update(anchor: TourAnchor, rect: Rect) {
        // Attached but not yet laid out: treated as absent so no spotlight lands at the origin.
        if (rect.width <= 0f || rect.height <= 0f) {
            bounds.remove(anchor)
        } else {
            bounds[anchor] = rect
        }
    }

    fun remove(anchor: TourAnchor) {
        bounds.remove(anchor)
        scrollers.remove(anchor)
    }

    internal fun registerScroller(anchor: TourAnchor, scroller: TourAnchorScroller) {
        scrollers[anchor] = scroller
    }

    suspend fun bringIntoView(anchor: TourAnchor) {
        scrollers[anchor]?.bringIntoView()
    }
}

internal fun interface TourAnchorScroller {
    suspend fun bringIntoView()
}

/** Empty by default, so [tourAnchor] is a no-op in previews and tests. */
val LocalTourAnchorRegistry = compositionLocalOf { TourAnchorRegistry() }

fun Modifier.tourAnchor(anchor: TourAnchor): Modifier = this then TourAnchorElement(anchor)

private data class TourAnchorElement(private val anchor: TourAnchor) : ModifierNodeElement<TourAnchorNode>() {
    override fun create(): TourAnchorNode = TourAnchorNode(anchor)

    override fun update(node: TourAnchorNode) {
        node.setAnchor(anchor)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "tourAnchor"
        value = anchor
    }
}

private class TourAnchorNode(private var anchor: TourAnchor) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var registry: TourAnchorRegistry? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val current = currentValueOf(LocalTourAnchorRegistry)
        registry = current
        current.update(anchor, coordinates.boundsOnScreen())
        current.registerScroller(anchor) { if (isAttached) bringIntoView() }
    }

    override fun onDetach() {
        registry?.remove(anchor)
        registry = null
    }

    fun setAnchor(newAnchor: TourAnchor) {
        if (newAnchor == anchor) return
        registry?.remove(anchor)
        anchor = newAnchor
    }
}

@Composable
fun rememberTourAnchorRegistry(): TourAnchorRegistry = remember { TourAnchorRegistry() }

/** See [TourAnchorRegistry] for why screen space. */
internal fun LayoutCoordinates.boundsOnScreen(): Rect {
    val topLeft = positionOnScreen()
    return Rect(topLeft, Size(size.width.toFloat(), size.height.toFloat()))
}
