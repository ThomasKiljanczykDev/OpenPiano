package dev.thomas_kiljanczyk.openpiano

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dagger.hilt.android.AndroidEntryPoint
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenPianoTheme {
                Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                    OpenPianoNavHost()
                }
            }
        }
    }

    // uiMode is in configChanges, so the window background is not re-resolved for night mode on its own.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.setBackgroundDrawableResource(R.color.window_background)
    }
}
