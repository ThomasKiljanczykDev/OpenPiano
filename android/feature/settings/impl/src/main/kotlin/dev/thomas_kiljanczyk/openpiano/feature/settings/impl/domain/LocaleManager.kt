package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

/** Implemented in `app`, where the Android per-app-language APIs actually live. */
interface LocaleManager {
    fun getSavedLanguage(): LanguageOption

    fun updateLanguage(language: LanguageOption)
}
