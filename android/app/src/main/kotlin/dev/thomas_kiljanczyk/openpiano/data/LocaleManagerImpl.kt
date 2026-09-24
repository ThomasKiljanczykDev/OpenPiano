package dev.thomas_kiljanczyk.openpiano.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.thomas_kiljanczyk.openpiano.core.common.di.ApplicationScope
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.SupportedLanguages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager as LocaleManagerInterface

/**
 * Below API 33 an explicit user choice is persisted in [UserPreferencesRepository]; otherwise the device
 * language is re-resolved on every start. On 33+ the framework owns the choice.
 */
@Singleton
class LocaleManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) : LocaleManagerInterface {

    @Volatile
    private var chosenTag: String? = null

    override fun getSavedLanguage(): LanguageOption =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val supported = SupportedLanguages.localeTags(context.resources)
            chosenTag?.takeIf { it in supported }?.let(::LanguageOption)
                ?: SupportedLanguages.resolveForDevice(context)
        } else {
            val applied = frameworkLocaleManager().applicationLocales
            if (applied.isEmpty) {
                LanguageOption.SYSTEM
            } else {
                LanguageOption(SupportedLanguages.matchTag(applied[0].toLanguageTag(), context.resources))
            }
        }

    override fun updateLanguage(language: LanguageOption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // AppCompatDelegate no-ops on 33+ while no AppCompat activity delegate is alive.
            frameworkLocaleManager().applicationLocales =
                language.localeTag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        } else {
            chosenTag = language.localeTag
            scope.launch { userPreferencesRepository.setLanguageTag(language.localeTag) }
            AppCompatDelegate.setApplicationLocales(language.toLocaleListCompat())
        }
    }

    /** Called from [dev.thomas_kiljanczyk.openpiano.OpenPianoApplication.onCreate]. */
    fun applyLocaleOnStartup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        // Must resolve before the first Activity picks its resources.
        chosenTag = runBlocking { userPreferencesRepository.getLanguageTag() }
        AppCompatDelegate.setApplicationLocales(getSavedLanguage().toLocaleListCompat())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun frameworkLocaleManager(): LocaleManager = context.getSystemService(LocaleManager::class.java)
}
