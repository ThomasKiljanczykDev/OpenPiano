package dev.thomas_kiljanczyk.openpiano

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var audioEngine: AudioEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = audioEngine.start()

                override fun onStop(owner: LifecycleOwner) = audioEngine.stop()

                override fun onDestroy(owner: LifecycleOwner) {
                    if (isFinishing && !isChangingConfigurations) {
                        audioEngine.release()
                    }
                }
            },
        )
        setContent {
            OpenPianoTheme {
                Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                    OpenPianoNavHost()
                }
            }
        }
    }
}
