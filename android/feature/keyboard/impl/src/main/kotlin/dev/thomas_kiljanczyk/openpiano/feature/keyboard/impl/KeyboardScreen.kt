package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import dev.thomas_kiljanczyk.openpiano.core.audio.DEFAULT_VELOCITY
import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.model.Note
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.ui.LockDisplayCutoutMode
import dev.thomas_kiljanczyk.openpiano.core.ui.LockScreenOrientation

private val OVERVIEW_HEIGHT = 48.dp

const val PIANO_KEYBOARD_TEST_TAG = "piano_keyboard"

@Composable
fun KeyboardRoute(viewModel: KeyboardViewModel = hiltViewModel(), onNavigateToSettings: () -> Unit) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE)
    LockDisplayCutoutMode(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    KeyboardScreen(
        uiState = uiState,
        engine = viewModel.audioEngine,
        midiOutputPort = viewModel.midiOutputPort,
        onShiftKeys = viewModel::shiftKeys,
        onLowestNoteChange = viewModel::setLowestNote,
        onOpenSettings = onNavigateToSettings,
    )
}

@Composable
fun KeyboardScreen(
    uiState: KeyboardUiState,
    engine: AudioEngine,
    midiOutputPort: MidiOutputPort,
    onShiftKeys: (Int) -> Unit,
    onLowestNoteChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.displayCutout)),
    ) {
        KeyboardControls(uiState = uiState, onOpenSettings = onOpenSettings)
        OverviewRow(
            uiState = uiState,
            onShiftKeys = onShiftKeys,
            onLowestNoteChange = onLowestNoteChange,
        )
        if (!uiState.soundReady) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.weight(1f).testTag(PIANO_KEYBOARD_TEST_TAG)) {
            val midiOutputEnabled = uiState.settings.midiOutputEnabled
            PianoKeyboard(
                lowestNote = uiState.lowestNote,
                whiteKeyCount = uiState.settings.visibleWhiteKeys,
                labelMode = uiState.settings.labelMode,
                useAreaHitTest = uiState.useAreaHitTest,
                areaOverlapThreshold = uiState.areaOverlapThreshold,
                onNoteOn = {
                    engine.noteOn(it, DEFAULT_VELOCITY)
                    if (midiOutputEnabled) midiOutputPort.noteOn(it, DEFAULT_VELOCITY)
                },
                onNoteOff = {
                    engine.noteOff(it)
                    if (midiOutputEnabled) midiOutputPort.noteOff(it)
                },
            )
        }
    }
}

@Composable
private fun OverviewRow(
    uiState: KeyboardUiState,
    onShiftKeys: (Int) -> Unit,
    onLowestNoteChange: (Int) -> Unit,
) {
    val overviewDescription = stringResource(R.string.keyboard_overview)
    Row(
        modifier = Modifier.fillMaxWidth().height(OVERVIEW_HEIGHT).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShiftButton(
            icon = R.drawable.keyboard_double_arrow_left,
            description = stringResource(R.string.keyboard_octave_down),
            enabled = uiState.canShiftDown,
            onClick = { onShiftKeys(-Piano.WHITE_KEYS_PER_OCTAVE) },
        )
        ShiftButton(
            icon = R.drawable.keyboard_arrow_left,
            description = stringResource(R.string.keyboard_key_down),
            enabled = uiState.canShiftDown,
            onClick = { onShiftKeys(-1) },
        )
        KeyboardOverview(
            lowestNote = uiState.lowestNote,
            visibleWhiteKeys = uiState.settings.visibleWhiteKeys,
            onLowestNoteChange = onLowestNoteChange,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 4.dp)
                .semantics { contentDescription = overviewDescription },
        )
        ShiftButton(
            icon = R.drawable.keyboard_arrow_right,
            description = stringResource(R.string.keyboard_key_up),
            enabled = uiState.canShiftUp,
            onClick = { onShiftKeys(1) },
        )
        ShiftButton(
            icon = R.drawable.keyboard_double_arrow_right,
            description = stringResource(R.string.keyboard_octave_up),
            enabled = uiState.canShiftUp,
            onClick = { onShiftKeys(Piano.WHITE_KEYS_PER_OCTAVE) },
        )
    }
}

@Composable
private fun ShiftButton(
    @DrawableRes icon: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = description,
        )
    }
}

@Composable
private fun KeyboardControls(uiState: KeyboardUiState, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = Note.fullName(uiState.lowestNote),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.settings),
                contentDescription = stringResource(R.string.keyboard_settings),
            )
        }
    }
}
