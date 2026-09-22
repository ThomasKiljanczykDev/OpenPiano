package dev.thomas_kiljanczyk.openpiano.di

import com.google.firebase.analytics.FirebaseAnalytics
import dev.thomas_kiljanczyk.openpiano.core.analytics.AnalyticsHelper
import javax.inject.Inject

internal class FirebaseAnalyticsHelper @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsHelper {
    override fun setCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
