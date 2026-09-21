package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

import androidx.core.os.LocaleListCompat

/**
 * One language-picker entry, by BCP-47 tag; null is "system default", offered on Android 13+ only.
 * Not an enum - the shipped set lives in `locale_config.xml`, see [SupportedLanguages].
 */
@JvmInline
value class LanguageOption(val localeTag: String?) {

    fun toLocaleListCompat(): LocaleListCompat =
        if (localeTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }

    companion object {
        val SYSTEM = LanguageOption(null)

        fun fromLocaleTag(tag: String?, supported: List<String>): LanguageOption =
            if (tag != null && tag in supported) LanguageOption(tag) else SYSTEM
    }
}
