package dev.thomas_kiljanczyk.openpiano.core.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val SPOTLIGHT_PADDING_DP = 8
private const val SPOTLIGHT_CORNER_DP = 16
private const val CARD_GAP_DP = 16
private const val CARD_MAX_WIDTH_DP = 400
private const val SCRIM_ALPHA = 0.72f
private const val ANCHOR_SETTLE_MILLIS = 350L
private const val TOUR_ENTER_FADE_MILLIS = 300

/** While [step] is non-null the overlay swallows input and Back (as [onSkip]) and [content] loses semantics. */
@Composable
fun TourHost(
    registry: TourAnchorRegistry,
    step: TourStep?,
    stepIndex: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    expansion: TourExpansion = TourExpansion.None,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTourAnchorRegistry provides registry,
        LocalTourExpansion provides expansion,
    ) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (step != null) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                content()
            }

            if (step != null) {
                BackHandler(onBack = onSkip)
                TourOverlay(
                    step = step,
                    stepIndex = stepIndex,
                    stepCount = stepCount,
                    registry = registry,
                    onNext = onNext,
                    onSkip = onSkip,
                )
            }
        }
    }
}

@Composable
private fun TourOverlay(
    step: TourStep,
    stepIndex: Int,
    stepCount: Int,
    registry: TourAnchorRegistry,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val bounds = step.anchors.mapNotNull { registry[it] }
    val allPresent = bounds.size == step.anchors.size

    // A missing anchor falls back to a centred card rather than skipping the step.
    // Previews never run effects, so they start settled and fully faded in.
    val inPreview = LocalInspectionMode.current
    var settled by remember(step) { mutableStateOf(inPreview) }
    LaunchedEffect(step, allPresent) {
        if (!allPresent) delay(ANCHOR_SETTLE_MILLIS)
        settled = true
    }

    // Keyed on presence, not bounds, so the scroll it causes does not restart it.
    val presentAnchors = step.anchors.filter { registry[it] != null }
    LaunchedEffect(step, presentAnchors) {
        presentAnchors.forEach { registry.bringIntoView(it) }
    }

    val fade = remember { Animatable(if (inPreview) 1f else 0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(TOUR_ENTER_FADE_MILLIS)) }

    // Anchors are screen space; rebase onto the overlay.
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val localBounds = bounds.map { it.translate(-rootOffset.x, -rootOffset.y) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionOnScreen() }
            .consumeAllPointerInput(key = step)
            .semantics { isTraversalGroup = true },
    ) {
        if (!settled) return@Box

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
            Scrim(localBounds)
            TourCardLayout(
                step = step,
                stepIndex = stepIndex,
                stepCount = stepCount,
                anchor = localBounds.union(),
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
private fun Scrim(cutouts: List<Rect>) {
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val density = LocalDensity.current
    val padding = with(density) { SPOTLIGHT_PADDING_DP.dp.toPx() }
    val corner = with(density) { SPOTLIGHT_CORNER_DP.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            cutouts.forEach {
                addRoundRect(RoundRect(it.inflate(padding), CornerRadius(corner)))
            }
        }
        drawPath(path, scrimColor)
    }
}

@Composable
private fun TourCardLayout(
    step: TourStep,
    stepIndex: Int,
    stepCount: Int,
    anchor: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    val cardGap = with(density) { CARD_GAP_DP.dp.toPx() }
    val cardMaxWidth = with(density) { CARD_MAX_WIDTH_DP.dp.roundToPx() }
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val insetTop = with(density) { safeInsets.calculateTopPadding().toPx() }
    val insetBottom = with(density) { safeInsets.calculateBottomPadding().toPx() }

    Layout(
        content = { TourCard(step, stepIndex, stepCount, onNext, onSkip) },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            Constraints(
                maxWidth = minOf(cardMaxWidth, constraints.maxWidth),
                maxHeight = constraints.maxHeight,
            ),
        )
        val (x, y) = TourCardPlacement.offsetFor(
            anchor = anchor,
            cardWidth = placeable.width.toFloat(),
            cardHeight = placeable.height.toFloat(),
            containerWidth = constraints.maxWidth.toFloat(),
            containerHeight = constraints.maxHeight.toFloat(),
            insetTop = insetTop,
            insetBottom = insetBottom,
            gap = cardGap,
            position = step.cardPosition,
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(x.toInt(), y.toInt())
        }
    }
}

private fun List<Rect>.union(): Rect? = reduceOrNull { acc, rect ->
    Rect(
        left = minOf(acc.left, rect.left),
        top = minOf(acc.top, rect.top),
        right = maxOf(acc.right, rect.right),
        bottom = maxOf(acc.bottom, rect.bottom),
    )
}

@Composable
private fun TourCard(
    step: TourStep,
    stepIndex: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val isLast = stepIndex >= stepCount - 1
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = CARD_MAX_WIDTH_DP.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(step.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.tutorial_step_counter, stepIndex + 1, stepCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.tutorial_skip))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onNext) {
                        Text(stringResource(if (isLast) R.string.tutorial_done else R.string.tutorial_next))
                    }
                }
            }
        }
    }
}

/** Pointers already consumed elsewhere are left alone until released. */
private fun Modifier.consumeAllPointerInput(key: Any?): Modifier = pointerInput(key) {
    val claimedElsewhere = mutableSetOf<Long>()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            event.changes.forEach {
                if (!it.pressed) claimedElsewhere.remove(it.id.value)
                if (it.isConsumed) claimedElsewhere.add(it.id.value)
                if (it.id.value !in claimedElsewhere) it.consume()
            }
        }
    }
}
