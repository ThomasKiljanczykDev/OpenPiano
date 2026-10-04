package dev.thomas_kiljanczyk.openpiano.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.tutorial.CURRENT_TUTORIAL_VERSION
import dev.thomas_kiljanczyk.openpiano.core.tutorial.shouldShowTutorial
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TutorialPhase {
    data object Loading : TutorialPhase

    data class Tour(val stepIndex: Int) : TutorialPhase

    data object None : TutorialPhase
}

/** Activity-scoped so rotation keeps the step; progress is not persisted. */
@HiltViewModel
class TutorialViewModel @Inject constructor(private val repository: UserPreferencesRepository) : ViewModel() {

    var phase: TutorialPhase by mutableStateOf(TutorialPhase.Loading)
        private set

    init {
        viewModelScope.launch {
            // Collected continuously so replay restarts the tour live.
            repository.tutorialCompletedVersion.collect { completedVersion ->
                if (phase is TutorialPhase.Tour) return@collect
                phase = if (shouldShowTutorial(completedVersion)) TutorialPhase.Tour(0) else TutorialPhase.None
            }
        }
    }

    fun next(stepCount: Int) {
        val current = phase as? TutorialPhase.Tour ?: return
        if (current.stepIndex >= stepCount - 1) {
            finish()
        } else {
            phase = TutorialPhase.Tour(current.stepIndex + 1)
        }
    }

    fun finish() {
        phase = TutorialPhase.None
        viewModelScope.launch { repository.setTutorialCompletedVersion(CURRENT_TUTORIAL_VERSION) }
    }
}
