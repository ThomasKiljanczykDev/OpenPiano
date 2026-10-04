package dev.thomas_kiljanczyk.openpiano.tutorial

import dev.thomas_kiljanczyk.openpiano.core.testing.FakeUserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.testing.MainDispatcherRule
import dev.thomas_kiljanczyk.openpiano.core.tutorial.CURRENT_TUTORIAL_VERSION
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TutorialViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserPreferencesRepository()

    private fun viewModel() = TutorialViewModel(repository)

    @Test
    fun `fresh install starts the tour at the first step`() {
        assertEquals(TutorialPhase.Tour(0), viewModel().phase)
    }

    @Test
    fun `completed version shows no tour`() {
        runTest { repository.setTutorialCompletedVersion(CURRENT_TUTORIAL_VERSION) }
        assertEquals(TutorialPhase.None, viewModel().phase)
    }

    @Test
    fun `next advances one step`() {
        val viewModel = viewModel()
        viewModel.next(STEP_COUNT)
        assertEquals(TutorialPhase.Tour(1), viewModel.phase)
    }

    @Test
    fun `next on the last step finishes and records the version`() {
        val viewModel = viewModel()
        repeat(STEP_COUNT) { viewModel.next(STEP_COUNT) }
        assertEquals(TutorialPhase.None, viewModel.phase)
        assertEquals(CURRENT_TUTORIAL_VERSION, repository.tutorialCompletedVersion.value)
    }

    @Test
    fun `finish records the version`() {
        val viewModel = viewModel()
        viewModel.next(STEP_COUNT)
        viewModel.finish()
        assertEquals(TutorialPhase.None, viewModel.phase)
        assertEquals(CURRENT_TUTORIAL_VERSION, repository.tutorialCompletedVersion.value)
    }

    @Test
    fun `replay after completion restarts at the first step`() = runTest {
        val viewModel = viewModel()
        viewModel.finish()
        repository.setTutorialCompletedVersion(0)
        assertEquals(TutorialPhase.Tour(0), viewModel.phase)
    }

    @Test
    fun `emissions while touring are ignored`() = runTest {
        val viewModel = viewModel()
        viewModel.next(STEP_COUNT)
        repository.setTutorialCompletedVersion(CURRENT_TUTORIAL_VERSION)
        repository.setTutorialCompletedVersion(0)
        assertEquals(TutorialPhase.Tour(1), viewModel.phase)
    }

    private companion object {
        const val STEP_COUNT = 3
    }
}
