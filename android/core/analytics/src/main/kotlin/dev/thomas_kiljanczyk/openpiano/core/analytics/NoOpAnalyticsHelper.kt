package dev.thomas_kiljanczyk.openpiano.core.analytics

object NoOpAnalyticsHelper : AnalyticsHelper {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
