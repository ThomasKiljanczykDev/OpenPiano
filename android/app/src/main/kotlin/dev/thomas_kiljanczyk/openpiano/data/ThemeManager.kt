package dev.thomas_kiljanczyk.openpiano.data

import androidx.appcompat.app.AppCompatDelegate
import dev.thomas_kiljanczyk.openpiano.core.common.allowingThreadDiskReads
import dev.thomas_kiljanczyk.openpiano.core.common.di.ApplicationScope
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    fun applyThemeOnStartup() {
        // Must apply before the first Activity inflates to avoid a system-mode flash.
        val initial = allowingThreadDiskReads {
            runBlocking { userPreferencesRepository.themeMode.first() }
        }
        AppCompatDelegate.setDefaultNightMode(initial.toNightMode())
        scope.launch(Dispatchers.Main) {
            userPreferencesRepository.themeMode.collect { AppCompatDelegate.setDefaultNightMode(it.toNightMode()) }
        }
    }
}

internal fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
}
