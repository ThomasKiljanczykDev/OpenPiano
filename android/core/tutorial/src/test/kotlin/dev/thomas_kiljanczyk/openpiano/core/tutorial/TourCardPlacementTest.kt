package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TourCardPlacementTest {

    private val cardWidth = 400f
    private val cardHeight = 300f
    private val landscapeCardHeight = 160f
    private val gap = 16f

    private fun place(
        anchor: Rect?,
        containerWidth: Float = 1000f,
        containerHeight: Float = 2000f,
        insetTop: Float = 0f,
        insetBottom: Float = 0f,
        position: TourCardPosition = TourCardPosition.AUTO,
        cardHeight: Float = this.cardHeight,
    ) = TourCardPlacement.offsetFor(
        anchor = anchor,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        insetTop = insetTop,
        insetBottom = insetBottom,
        gap = gap,
        position = position,
    )

    @Test
    fun `anchor near the top places the card below it`() {
        val anchor = Rect(Offset(400f, 100f), Size(200f, 100f))

        assertEquals(anchor.bottom + gap, place(anchor).second, 0f)
    }

    @Test
    fun `anchor near the bottom places the card above it`() {
        val anchor = Rect(Offset(400f, 1850f), Size(200f, 100f))

        assertEquals(anchor.top - gap - cardHeight, place(anchor).second, 0f)
    }

    @Test
    fun `card is horizontally centred on the anchor`() {
        val anchor = Rect(Offset(400f, 100f), Size(200f, 100f))

        assertEquals(anchor.center.x - cardWidth / 2f, place(anchor).first, 0f)
    }

    @Test
    fun `card is clamped into the container when the anchor sits at an edge`() {
        val (x, _) = place(Rect(Offset(0f, 900f), Size(80f, 80f)))

        assertTrue(x >= 0f)
        assertTrue(x + cardWidth <= 1000f)
    }

    @Test
    fun `card never lands under the bottom inset`() {
        val (_, y) = place(Rect(Offset(400f, 100f), Size(200f, 100f)), insetBottom = 1500f)

        assertTrue(y + cardHeight <= 2000f - 1500f)
    }

    @Test
    fun `BOTTOM pins the card to the bottom edge`() {
        val (_, y) = place(Rect(Offset(900f, 40f), Size(80f, 80f)), position = TourCardPosition.BOTTOM)

        assertEquals(2000f - cardHeight, y, 0f)
    }

    @Test
    fun `TOP pins the card to the top edge`() {
        val (_, y) = place(Rect(Offset(850f, 1850f), Size(100f, 100f)), position = TourCardPosition.TOP)

        assertEquals(0f, y, 0f)
    }

    @Test
    fun `a pinned position respects insets`() {
        val anchor = Rect(Offset(900f, 40f), Size(80f, 80f))

        val top = place(anchor, insetTop = 48f, position = TourCardPosition.TOP).second
        val bottom = place(anchor, insetBottom = 200f, position = TourCardPosition.BOTTOM).second

        assertEquals(48f, top, 0f)
        assertEquals(2000f - 200f - cardHeight, bottom, 0f)
    }

    @Test
    fun `a pinned position still centres horizontally on the anchor`() {
        val anchor = Rect(Offset(300f, 40f), Size(80f, 80f))

        assertEquals(
            anchor.center.x - cardWidth / 2f,
            place(anchor, position = TourCardPosition.BOTTOM).first,
            0f,
        )
    }

    @Test
    fun `anchorless steps are centred`() {
        val (x, y) = place(anchor = null)

        assertEquals((1000f - cardWidth) / 2f, x, 0f)
        assertEquals((2000f - cardHeight) / 2f, y, 0f)
    }

    @Test
    fun `short landscape container places the card below a top anchor`() {
        val anchor = Rect(Offset(700f, 8f), Size(48f, 48f))

        val (_, y) = place(anchor, 800f, 360f, cardHeight = landscapeCardHeight)

        assertEquals(anchor.bottom + gap, y, 0f)
    }

    @Test
    fun `short landscape container places the card above a bottom anchor`() {
        val anchor = Rect(Offset(100f, 300f), Size(200f, 48f))

        val (_, y) = place(anchor, 800f, 360f, cardHeight = landscapeCardHeight)

        assertEquals(anchor.top - gap - landscapeCardHeight, y, 0f)
    }

    @Test
    fun `short landscape container with a tall anchor stays inside the insets`() {
        val anchor = Rect(Offset(0f, 40f), Size(800f, 280f))

        val (_, y) = place(anchor, 800f, 360f, insetTop = 24f, insetBottom = 24f, cardHeight = landscapeCardHeight)

        assertTrue(y >= 24f)
        assertTrue(y + landscapeCardHeight <= 360f - 24f)
    }

    @Test
    fun `short landscape container keeps a pinned top card inside the insets`() {
        val anchor = Rect(Offset(0f, 100f), Size(800f, 200f))

        val (_, y) = place(
            anchor,
            800f,
            360f,
            insetTop = 24f,
            position = TourCardPosition.TOP,
            cardHeight = landscapeCardHeight,
        )

        assertEquals(24f, y, 0f)
    }

    @Test
    fun `card taller than the container is anchored at the top inset`() {
        val (_, y) = place(null, containerWidth = 800f, containerHeight = 200f, insetTop = 10f)

        assertEquals(10f, y, 0f)
    }
}
