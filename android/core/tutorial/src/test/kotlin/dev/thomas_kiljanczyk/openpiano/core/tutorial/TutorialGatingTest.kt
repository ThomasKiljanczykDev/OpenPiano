package dev.thomas_kiljanczyk.openpiano.core.tutorial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialGatingTest {

    @Test
    fun `fresh install shows the tutorial`() {
        assertTrue(shouldShowTutorial(completedVersion = 0))
    }

    @Test
    fun `current version is not shown again`() {
        assertFalse(shouldShowTutorial(completedVersion = CURRENT_TUTORIAL_VERSION))
    }

    @Test
    fun `newer stored version is not shown`() {
        assertFalse(shouldShowTutorial(completedVersion = CURRENT_TUTORIAL_VERSION + 3))
    }

    @Test
    fun `older stored version is shown`() {
        assertTrue(shouldShowTutorial(completedVersion = CURRENT_TUTORIAL_VERSION - 1))
    }
}
