package dev.thomas_kiljanczyk.openpiano.core.analytics

import org.junit.Test

class NoOpAnalyticsHelperTest {

    @Test
    fun `setCollectionEnabled does nothing`() {
        NoOpAnalyticsHelper.setCollectionEnabled(true)
        NoOpAnalyticsHelper.setCollectionEnabled(false)
    }
}
