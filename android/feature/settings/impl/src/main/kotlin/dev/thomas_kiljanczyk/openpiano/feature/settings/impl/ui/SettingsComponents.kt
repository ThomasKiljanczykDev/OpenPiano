package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SettingsTileMinHeight = 64.dp

@Composable
fun SettingsCardGroup(modifier: Modifier = Modifier, content: @Composable SettingsCardGroupScope.() -> Unit) {
    val scope = SettingsCardGroupScopeImpl()
    scope.content()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        scope.items.forEachIndexed { index, item ->
            val isFirst = index == 0
            val isLast = index == scope.items.lastIndex

            if (!isFirst) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = when {
                    isFirst && isLast -> RoundedCornerShape(16.dp)

                    isFirst -> RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp,
                    )

                    isLast -> RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    )

                    else -> RoundedCornerShape(4.dp)
                },
            ) {
                item()
            }
        }
    }
}

interface SettingsCardGroupScope {
    fun item(content: @Composable () -> Unit)
}

private class SettingsCardGroupScopeImpl : SettingsCardGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
fun <T> SettingsRowWithRadioButtonGroupDialog(
    title: String,
    value: T,
    options: List<Pair<T, String>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else 0.38f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsTileMinHeight)
            .clickable(
                enabled = enabled,
                onClick = { showDialog = true },
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
        )
        Text(
            text = subtitle ?: options.find { it.first == value }?.second.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
        )
    }

    if (showDialog && enabled) {
        SettingsRadioButtonGroupDialog(
            title = title,
            options = options,
            selectedValue = value,
            onOptionSelected = onValueChange,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
fun SettingsRowButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsTileMinHeight)
            .clickable(
                onClick = onClick,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsTileMinHeight)
            .combinedClickable(
                onClick = { onCheckedChange(!checked) },
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
            )
            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f).padding(end = 16.dp),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .combinedClickable(
                    onClick = { onCheckedChange(!checked) },
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false),
                )
                .padding(4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(
    title: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: (Int) -> String = Int::toString,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Holds the drag position, and the committed value until the store echoes it back.
    var pending by remember(value) { mutableStateOf<Float?>(null) }
    val shownValue = pending?.roundToInt() ?: value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsTileMinHeight)
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Slider(
                    value = pending ?: value.toFloat(),
                    onValueChange = { pending = it },
                    onValueChangeFinished = {
                        val committed = pending?.roundToInt()
                        if (committed == null || committed == value) {
                            pending = null
                        } else {
                            onValueChange(committed)
                        }
                    },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = valueRange.last - valueRange.first - 1,
                    interactionSource = interactionSource,
                    modifier = Modifier.weight(1f),
                    thumb = {
                        SliderDefaults.Thumb(
                            modifier = Modifier.height(20.dp),
                            interactionSource = interactionSource,
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(12.dp))
                    },
                )
            }
            Text(
                text = valueLabel(shownValue),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
