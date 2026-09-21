package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedLanguagesTest {

    @Test
    fun `autonym is already capitalised for languages that start uppercase`() {
        assertEquals("Deutsch", SupportedLanguages.autonym("de"))
        assertEquals("English", SupportedLanguages.autonym("en"))
    }

    @Test
    fun `autonym capitalises languages whose display name starts lowercase`() {
        assertEquals("Polski", SupportedLanguages.autonym("pl"))
        assertEquals("Español", SupportedLanguages.autonym("es"))
        assertEquals("Français", SupportedLanguages.autonym("fr"))
    }

    @Test
    fun `autonym for a region tag uses the language, not the region`() {
        assertEquals("Português", SupportedLanguages.autonym("pt-BR"))
    }
}
