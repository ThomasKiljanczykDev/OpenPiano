package dev.thomas_kiljanczyk.openpiano

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.thomas_kiljanczyk.openpiano.core.testing.AudioEvent
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeAudioEngine
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeUserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.tutorial.CURRENT_TUTORIAL_VERSION
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.PIANO_KEYBOARD_TEST_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject
import dev.thomas_kiljanczyk.openpiano.core.tutorial.R as TutorialR
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.R as KeyboardR
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.R as SettingsR

private const val STEP_COUNT = 11
private const val FIRST_SETTINGS_STEP = 7
private const val KEY_TAP_X_FRACTION = 0.3f
private const val KEY_TAP_Y_FRACTION = 0.9f

private val TITLES = listOf(
    R.string.tour_keyboard_welcome_title,
    R.string.tour_keyboard_keys_title,
    R.string.tour_keyboard_overview_title,
    R.string.tour_keyboard_shift_title,
    R.string.tour_keyboard_lowest_note_title,
    R.string.tour_keyboard_settings_title,
    R.string.tour_settings_intro_title,
    R.string.tour_settings_visible_keys_title,
    R.string.tour_settings_touch_mode_title,
    R.string.tour_settings_touch_precision_title,
    R.string.tour_settings_replay_title,
)

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TutorialFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var repository: FakeUserPreferencesRepository

    @Inject
    lateinit var engine: FakeAudioEngine

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = hiltRule.inject()

    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun assertStep(index: Int) {
        composeRule.onNodeWithText(str(TITLES[index - 1])).assertExists()
        composeRule.onNodeWithText(str(TutorialR.string.tutorial_step_counter, index, STEP_COUNT)).assertExists()
    }

    private fun keyboardComposed(): Boolean = composeRule
        .onAllNodes(hasTestTag(PIANO_KEYBOARD_TEST_TAG), useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()

    private fun assertKeyboardComposed(expected: Boolean) {
        composeRule.waitUntil(WAIT_MS) { keyboardComposed() == expected }
    }

    private fun clickNext() = composeRule.onNodeWithText(str(TutorialR.string.tutorial_next)).performClick()

    private fun clickSkip() = composeRule.onNodeWithText(str(TutorialR.string.tutorial_skip)).performClick()

    private fun tapKeysArea() = composeRule.onRoot().performTouchInput {
        click(Offset(width * KEY_TAP_X_FRACTION, height * KEY_TAP_Y_FRACTION))
    }

    private fun advanceTo(step: Int) {
        repeat(step - 1) { clickNext() }
        assertStep(step)
    }

    @Test
    fun freshInstallShowsFirstStep() {
        assertStep(1)
    }

    @Test
    fun walkingAllStepsHopsToSettingsAndBackToKeyboard() {
        assertStep(1)
        for (step in 2..STEP_COUNT) {
            clickNext()
            assertStep(step)
            assertKeyboardComposed(expected = step < FIRST_SETTINGS_STEP)
        }

        composeRule.onNodeWithText(str(TutorialR.string.tutorial_done)).performClick()

        composeRule.onNodeWithTag(PIANO_KEYBOARD_TEST_TAG).assertExists()
        composeRule.onNodeWithText(str(TutorialR.string.tutorial_done)).assertDoesNotExist()
        assertEquals(CURRENT_TUTORIAL_VERSION, repository.tutorialCompletedVersion.value)
    }

    @Test
    fun skipOnSettingsStepReturnsToKeyboardAndRecordsVersion() {
        advanceTo(8)
        assertKeyboardComposed(expected = false)

        clickSkip()

        composeRule.onNodeWithTag(PIANO_KEYBOARD_TEST_TAG).assertExists()
        assertEquals(CURRENT_TUTORIAL_VERSION, repository.tutorialCompletedVersion.value)
    }

    @Test
    fun replayFromSettingsRestartsAtFirstStep() {
        clickSkip()
        composeRule.onNodeWithContentDescription(str(KeyboardR.string.keyboard_settings)).performClick()

        composeRule.onNodeWithText(str(SettingsR.string.settings_replay_tutorial)).performScrollTo().performClick()

        assertStep(1)
        assertKeyboardComposed(expected = true)
        assertTrue(repository.tutorialCompletedVersion.value < CURRENT_TUTORIAL_VERSION)
    }

    @Test
    fun tapsDuringTourDoNotReachKeys() {
        advanceTo(2)
        tapKeysArea()
        composeRule.waitForIdle()

        assertTrue(engine.events.none { it is AudioEvent.NoteOn })
        assertStep(2)

        clickSkip()
        tapKeysArea()
        composeRule.waitForIdle()

        assertTrue(engine.events.any { it is AudioEvent.NoteOn })
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
