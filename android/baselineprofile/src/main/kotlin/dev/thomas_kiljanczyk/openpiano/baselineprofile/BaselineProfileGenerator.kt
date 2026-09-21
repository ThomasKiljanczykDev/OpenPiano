package dev.thomas_kiljanczyk.openpiano.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.PIANO_KEYBOARD_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "dev.thomas_kiljanczyk.openpiano"
private const val SETTINGS_LABEL = "Settings"
private const val KEY_TAP_INSET_PX = 40
private const val WAIT_TIMEOUT_MS = 5_000L

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun tapKey() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()

        val keyboardSelector = By.res(PIANO_KEYBOARD_TEST_TAG)
        device.wait(Until.hasObject(keyboardSelector), WAIT_TIMEOUT_MS)
        val bounds = device.findObject(keyboardSelector).visibleBounds
        device.click(bounds.left + KEY_TAP_INSET_PX, bounds.centerY())
        device.waitForIdle()
    }

    @Test
    fun navigateToSettings() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()

        val settingsIconSelector = By.desc(SETTINGS_LABEL)
        device.wait(Until.hasObject(settingsIconSelector), WAIT_TIMEOUT_MS)
        device.findObject(settingsIconSelector).click()
        device.wait(Until.hasObject(By.text(SETTINGS_LABEL)), WAIT_TIMEOUT_MS)
    }
}
