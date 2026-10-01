package dev.thomas_kiljanczyk.openpiano.data

import androidx.appcompat.app.AppCompatDelegate
import dev.thomas_kiljanczyk.openpiano.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {

    @Test
    fun `theme modes map to AppCompat night modes`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.SYSTEM.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.LIGHT.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.DARK.toNightMode())
    }
}
