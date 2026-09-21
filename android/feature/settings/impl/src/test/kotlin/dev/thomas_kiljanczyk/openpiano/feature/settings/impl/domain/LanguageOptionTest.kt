package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageOptionTest {

    private val supported = listOf("en", "pl", "es", "pt-BR", "de", "fr")

    @Test
    fun `fromLocaleTag keeps a shipped tag`() {
        assertEquals(LanguageOption("pl"), LanguageOption.fromLocaleTag("pl", supported))
    }

    @Test
    fun `fromLocaleTag falls back to system for an unshipped tag`() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromLocaleTag("tr", supported))
    }

    @Test
    fun `fromLocaleTag falls back to system for a null tag`() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromLocaleTag(null, supported))
    }

    @Test
    fun `system option wraps a null tag`() {
        assertEquals(null, LanguageOption.SYSTEM.localeTag)
    }
}
