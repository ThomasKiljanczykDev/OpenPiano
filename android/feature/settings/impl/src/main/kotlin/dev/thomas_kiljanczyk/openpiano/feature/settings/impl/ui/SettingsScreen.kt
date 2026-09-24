package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.R
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.SupportedLanguages

private const val DONATE_URL = "https://buymeacoffee.com/thomas.kiljanczyk.dev"
private const val PRIVACY_POLICY_URL = "https://thomaskiljanczykdev.github.io/OpenPiano/privacy/"

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel(), onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The app locale can change outside this screen (system App language, backup restore).
    LifecycleResumeEffect(LocalConfiguration.current.locales) {
        viewModel.refreshLanguage()
        onPauseOrDispose {}
    }
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val languageOptions = remember(viewModel.languageOptions, systemDefaultLabel) {
        viewModel.languageOptions.map { option ->
            option to (option.localeTag?.let(SupportedLanguages::autonym) ?: systemDefaultLabel)
        }
    }
    SettingsScreen(
        uiState = uiState,
        language = viewModel.language,
        languageOptions = languageOptions,
        onLanguageChange = viewModel::selectLanguage,
        onLabelModeChange = viewModel::setLabelMode,
        onVisibleWhiteKeysChange = viewModel::setVisibleWhiteKeys,
        onReverbEnabledChange = viewModel::setReverbEnabled,
        onMidiOutputEnabledChange = viewModel::setMidiOutputEnabled,
        onTouchHitTestModeChange = viewModel::setTouchHitTestMode,
        onAreaOverlapThresholdPercentChange = viewModel::setAreaOverlapThresholdPercent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState?,
    language: LanguageOption,
    languageOptions: List<Pair<LanguageOption, String>>,
    onLanguageChange: (LanguageOption) -> Unit,
    onLabelModeChange: (KeyLabelMode) -> Unit,
    onVisibleWhiteKeysChange: (Int) -> Unit,
    onReverbEnabledChange: (Boolean) -> Unit,
    onMidiOutputEnabledChange: (Boolean) -> Unit,
    onTouchHitTestModeChange: (TouchHitTestMode) -> Unit,
    onAreaOverlapThresholdPercentChange: (Int) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.settings_navigate_up),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        if (uiState == null) return@Scaffold
        SettingsCardGroupContent(
            modifier = Modifier.padding(contentPadding).padding(16.dp),
            uiState = uiState,
            language = language,
            languageOptions = languageOptions,
            onLanguageChange = onLanguageChange,
            onLabelModeChange = onLabelModeChange,
            onVisibleWhiteKeysChange = onVisibleWhiteKeysChange,
            onReverbEnabledChange = onReverbEnabledChange,
            onMidiOutputEnabledChange = onMidiOutputEnabledChange,
            onTouchHitTestModeChange = onTouchHitTestModeChange,
            onAreaOverlapThresholdPercentChange = onAreaOverlapThresholdPercentChange,
        )
    }
}

@Composable
private fun SettingsCardGroupContent(
    uiState: SettingsUiState,
    language: LanguageOption,
    languageOptions: List<Pair<LanguageOption, String>>,
    onLanguageChange: (LanguageOption) -> Unit,
    onLabelModeChange: (KeyLabelMode) -> Unit,
    onVisibleWhiteKeysChange: (Int) -> Unit,
    onReverbEnabledChange: (Boolean) -> Unit,
    onMidiOutputEnabledChange: (Boolean) -> Unit,
    onTouchHitTestModeChange: (TouchHitTestMode) -> Unit,
    onAreaOverlapThresholdPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCardGroup(modifier = modifier) {
        item {
            SettingsRowWithRadioButtonGroupDialog(
                title = stringResource(R.string.settings_language),
                value = language,
                options = languageOptions,
                onValueChange = onLanguageChange,
            )
        }
        item {
            LabelModeRow(labelMode = uiState.settings.labelMode, onLabelModeChange = onLabelModeChange)
        }
        item {
            SettingsSlider(
                title = stringResource(R.string.settings_visible_keys),
                value = uiState.settings.visibleWhiteKeys,
                valueRange = KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE,
                onValueChange = onVisibleWhiteKeysChange,
            )
        }
        item {
            SettingsCheckbox(
                title = stringResource(R.string.settings_reverb),
                checked = uiState.settings.reverbEnabled,
                onCheckedChange = onReverbEnabledChange,
            )
        }
        item {
            MidiOutputRow(
                checked = uiState.settings.midiOutputEnabled,
                connected = uiState.midiOutputConnected,
                onCheckedChange = onMidiOutputEnabledChange,
            )
        }
        item {
            TouchHitTestModeRow(
                mode = uiState.settings.touchHitTestMode,
                supported = uiState.areaModeSupported,
                onModeChange = onTouchHitTestModeChange,
            )
        }
        item {
            TouchPrecisionRow(
                visible = uiState.areaModeSupported && uiState.settings.touchHitTestMode == TouchHitTestMode.AREA,
                value = uiState.settings.areaOverlapThresholdPercent,
                onValueChange = onAreaOverlapThresholdPercentChange,
            )
        }
        item { PrivacyPolicyRow() }
        item { DonateRow() }
    }
}

@Composable
private fun TouchPrecisionRow(visible: Boolean, value: Int, onValueChange: (Int) -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        SettingsSlider(
            title = stringResource(R.string.settings_touch_precision),
            value = value,
            valueRange = KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE,
            onValueChange = onValueChange,
            valueLabel = { "$it%" },
        )
    }
}

@Composable
private fun PrivacyPolicyRow() {
    val context = LocalContext.current
    SettingsRowButton(
        title = stringResource(R.string.settings_privacy_policy),
        subtitle = stringResource(R.string.settings_privacy_policy_summary),
        onClick = { context.openUrl(PRIVACY_POLICY_URL) },
    )
}

@Composable
private fun DonateRow() {
    val context = LocalContext.current
    SettingsRowButton(
        title = stringResource(R.string.settings_donate),
        subtitle = stringResource(R.string.settings_donate_summary),
        onClick = { context.openUrl(DONATE_URL) },
        icon = ImageVector.vectorResource(R.drawable.coffee),
    )
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
private fun LabelModeRow(labelMode: KeyLabelMode, onLabelModeChange: (KeyLabelMode) -> Unit) {
    SettingsRowWithRadioButtonGroupDialog(
        title = stringResource(R.string.settings_label_mode),
        value = labelMode,
        options = listOf(
            KeyLabelMode.NONE to stringResource(R.string.settings_label_mode_none),
            KeyLabelMode.C_ONLY to stringResource(R.string.settings_label_mode_c_only),
            KeyLabelMode.ALL to stringResource(R.string.settings_label_mode_all),
        ),
        onValueChange = onLabelModeChange,
    )
}

@Composable
private fun TouchHitTestModeRow(
    mode: TouchHitTestMode,
    supported: Boolean,
    onModeChange: (TouchHitTestMode) -> Unit,
) {
    SettingsRowWithRadioButtonGroupDialog(
        title = stringResource(R.string.settings_touch_hit_test_mode),
        value = mode,
        options = listOf(
            TouchHitTestMode.POINT to stringResource(R.string.settings_touch_hit_test_mode_point),
            TouchHitTestMode.AREA to stringResource(R.string.settings_touch_hit_test_mode_area),
        ),
        onValueChange = onModeChange,
        enabled = supported,
        subtitle = if (supported) null else stringResource(R.string.settings_touch_hit_test_mode_unsupported),
    )
}

@Composable
private fun MidiOutputRow(checked: Boolean, connected: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val status = stringResource(
        if (connected) R.string.settings_midi_output_connected else R.string.settings_midi_output_not_connected,
    )
    SettingsCheckbox(
        title = "${stringResource(R.string.settings_midi_output)} ($status)",
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}
