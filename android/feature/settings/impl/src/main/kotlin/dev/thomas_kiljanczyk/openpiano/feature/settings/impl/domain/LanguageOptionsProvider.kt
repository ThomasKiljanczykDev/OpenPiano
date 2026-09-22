package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface LanguageOptionsProvider {
    fun options(): List<Pair<LanguageOption, String>>
}

class ResourceLanguageOptionsProvider @Inject constructor(@param:ApplicationContext private val context: Context) :
    LanguageOptionsProvider {
    override fun options(): List<Pair<LanguageOption, String>> = SupportedLanguages.options(context)
}
