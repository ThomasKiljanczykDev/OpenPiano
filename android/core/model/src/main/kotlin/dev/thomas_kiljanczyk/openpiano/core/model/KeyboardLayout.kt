package dev.thomas_kiljanczyk.openpiano.core.model

data class KeyRect(
    val midiNote: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
}

/**
 * Pure geometry for a piano keyboard. Black keys are centred on the boundary between their two
 * white neighbours, which is close enough to a real instrument for v1.
 */
class KeyboardLayout(
    val lowestNote: Int,
    val whiteKeyCount: Int,
    val width: Float,
    val height: Float,
) {
    init {
        require(whiteKeyCount > 0) { "whiteKeyCount must be positive" }
        require(!Note.isBlack(lowestNote)) { "lowestNote must be a white key" }
    }

    val whiteKeyWidth: Float = width / whiteKeyCount
    val blackKeyWidth: Float = whiteKeyWidth * BLACK_WIDTH_RATIO
    val blackKeyHeight: Float = height * BLACK_HEIGHT_RATIO

    val whiteKeys: List<KeyRect> = buildList {
        var note = lowestNote
        repeat(whiteKeyCount) { index ->
            add(
                KeyRect(
                    midiNote = note,
                    left = index * whiteKeyWidth,
                    top = 0f,
                    right = (index + 1) * whiteKeyWidth,
                    bottom = height,
                ),
            )
            note = nextWhite(note)
        }
    }

    val blackKeys: List<KeyRect> = buildList {
        whiteKeys.dropLast(1).forEach { white ->
            val candidate = white.midiNote + 1
            if (Note.isBlack(candidate)) {
                add(
                    KeyRect(
                        midiNote = candidate,
                        left = white.right - blackKeyWidth / 2f,
                        top = 0f,
                        right = white.right + blackKeyWidth / 2f,
                        bottom = blackKeyHeight,
                    ),
                )
            }
        }
    }

    val highestNote: Int = whiteKeys.last().midiNote

    fun noteAt(x: Float, y: Float): Int? =
        blackKeys.firstOrNull { it.contains(x, y) }?.midiNote
            ?: whiteKeys.firstOrNull { it.contains(x, y) }?.midiNote

    // A qualifying black key excludes only the white key(s) beneath it; every other key is judged independently.
    fun notesInArea(
        x: Float,
        y: Float,
        touchMajor: Float,
        touchMinor: Float,
        overlapThreshold: Float = AREA_OVERLAP_THRESHOLD,
    ): Set<Int> {
        if (touchMajor <= 0f || touchMinor <= 0f) return setOfNotNull(noteAt(x, y))

        val touchLeft = x - touchMajor / 2f
        val touchRight = x + touchMajor / 2f
        val touchTop = y - touchMinor / 2f
        val touchBottom = y + touchMinor / 2f
        val touchArea = touchMajor * touchMinor

        fun overlapFraction(key: KeyRect): Float {
            val overlapWidth = (minOf(touchRight, key.right) - maxOf(touchLeft, key.left)).coerceAtLeast(0f)
            val overlapHeight = (minOf(touchBottom, key.bottom) - maxOf(touchTop, key.top)).coerceAtLeast(0f)
            return overlapWidth * overlapHeight / touchArea
        }

        val notes = mutableSetOf<Int>()
        val excludedWhiteNotes = mutableSetOf<Int>()

        blackKeys.forEach { black ->
            if (overlapFraction(black) >= overlapThreshold) {
                notes += black.midiNote
                whiteKeys.forEach { white ->
                    if (white.left < black.right && white.right > black.left) {
                        excludedWhiteNotes += white.midiNote
                    }
                }
            }
        }

        whiteKeys.forEach { white ->
            if (white.midiNote !in excludedWhiteNotes && overlapFraction(white) >= overlapThreshold) {
                notes += white.midiNote
            }
        }

        return notes.ifEmpty { setOfNotNull(noteAt(x, y)) }
    }

    private fun nextWhite(note: Int): Int {
        var next = note + 1
        while (Note.isBlack(next)) next++
        return next
    }

    companion object {
        const val BLACK_WIDTH_RATIO = 0.6f
        const val BLACK_HEIGHT_RATIO = 0.62f
        const val AREA_OVERLAP_THRESHOLD = 0.35f
    }
}
