package dev.thomas_kiljanczyk.openpiano.data

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.SupportedLanguages
import javax.inject.Inject
import javax.inject.Singleton
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager as LocaleManagerInterface

@Singleton
class LocaleManagerImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    LocaleManagerInterface {

    override fun getSavedLanguage(): LanguageOption =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Must resolve before the first Activity picks its resources; can't move off main thread.
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val tag = prefs.getString(LANGUAGE_KEY, null)
            LanguageOption.fromLocaleTag(tag, SupportedLanguages.localeTags(context.resources))
        } else {
            val applied = AppCompatDelegate.getApplicationLocales()
            if (applied.isEmpty) {
                LanguageOption.SYSTEM
            } else {
                val tag = SupportedLanguages.matchTag(
                    applied.toLanguageTags().substringBefore(','),
                    context.resources,
                )
                LanguageOption(tag)
            }
        }

    override fun updateLanguage(language: LanguageOption) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit { putString(LANGUAGE_KEY, language.localeTag) }
        }
        AppCompatDelegate.setApplicationLocales(language.toLocaleListCompat())
    }

    /** Called from [dev.thomas_kiljanczyk.openpiano.OpenPianoApplication.onCreate]. */
    fun applyLocaleOnStartup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        var saved = getSavedLanguage()
        if (saved.localeTag == null) {
            saved = SupportedLanguages.resolveForDevice(context)
            updateLanguage(saved)
        }
        AppCompatDelegate.setApplicationLocales(saved.toLocaleListCompat())
    }

    private companion object {
        const val PREFERENCES_NAME = "locale_prefs"
        const val LANGUAGE_KEY = "app_language"
    }
}
