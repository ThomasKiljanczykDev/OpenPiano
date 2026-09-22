package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

import android.content.Context
import android.content.res.Resources
import android.os.Build
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * The shipped languages, read from `locale_config.xml` so the in-app picker cannot drift from the
 * one Android itself offers in Settings > Apps > App language.
 */
object SupportedLanguages {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    /** Language tags in `locale_config.xml` order - the order the picker shows them in. */
    fun localeTags(resources: Resources): List<String> {
        val tags = mutableListOf<String>()
        resources.getXml(R.xml.locale_config).use { parser ->
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue(ANDROID_NS, "name")?.let(tags::add)
                }
                event = parser.next()
            }
        }
        return tags
    }

    /** Picker entries, each labelled with its autonym; "System default" only exists on API 33+. */
    fun options(context: Context): List<Pair<LanguageOption, String>> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(LanguageOption.SYSTEM to context.getString(R.string.settings_language_system_default))
        }
        localeTags(context.resources).forEach { tag -> add(LanguageOption(tag) to autonym(tag)) }
    }

    /** The language's own name, capitalised in *its* locale (`Locale.ROOT` breaks Turkish "i"). */
    fun autonym(localeTag: String): String {
        val locale = Locale.forLanguageTag(localeTag)
        return locale.getDisplayLanguage(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** Maps any tag onto a shipped one, so `en-US` selects `en`. Null when nothing matches. */
    fun matchTag(tag: String, resources: Resources): String? {
        val language = Locale.forLanguageTag(tag).language
        return localeTags(resources).firstOrNull { Locale.forLanguageTag(it).language == language }
    }

    /**
     * The starting option below API 33, where there is no per-app language setting and so no
     * SYSTEM option. Falls back to English, not to the first shipped language.
     */
    fun resolveForDevice(context: Context): LanguageOption {
        val deviceLanguage = Locale.getDefault().language
        val supported = localeTags(context.resources)
        val match = supported.firstOrNull { Locale.forLanguageTag(it).language == deviceLanguage }
        return LanguageOption(match ?: "en")
    }
}
