package dev.thomas_kiljanczyk.openpiano

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.StrictMode
import android.provider.Settings
import dagger.hilt.android.HiltAndroidApp
import dev.thomas_kiljanczyk.openpiano.core.analytics.AnalyticsHelper
import dev.thomas_kiljanczyk.openpiano.core.common.allowingThreadDiskReads
import dev.thomas_kiljanczyk.openpiano.data.LocaleManagerImpl
import javax.inject.Inject

@HiltAndroidApp
class OpenPianoApplication : Application() {

    @Inject
    lateinit var localeManager: LocaleManagerImpl

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate() {
        super.onCreate()

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        if (isDebuggable) {
            setupStrictMode()
        }

        // Excludes Firebase Test Lab / pre-launch report runs from analytics.
        val isRunningInFirebaseTestLab = allowingThreadDiskReads {
            Settings.System.getString(contentResolver, "firebase.test.lab") == "true"
        }
        analyticsHelper.setCollectionEnabled(!isDebuggable && !isRunningInFirebaseTestLab)

        localeManager.applyLocaleOnStartup()
    }

    private fun setupStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            threadPolicyBuilder.permitExplicitGc()
        }
        StrictMode.setThreadPolicy(
            threadPolicyBuilder.penaltyLog().build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build(),
        )
    }
}
