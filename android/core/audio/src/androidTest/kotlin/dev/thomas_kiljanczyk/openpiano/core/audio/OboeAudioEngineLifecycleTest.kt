package dev.thomas_kiljanczyk.openpiano.core.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT_MS = 10_000L
private const val POLL_MS = 20L

@RunWith(AndroidJUnit4::class)
class OboeAudioEngineLifecycleTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = OboeAudioEngine(
        InstrumentationRegistry.getInstrumentation().targetContext,
        scope,
        Dispatchers.IO,
    )

    @After
    fun tearDown() {
        engine.stop()
        scope.cancel()
    }

    @Test
    fun startAfterStopReopensTheStream() = runBlocking {
        engine.start()
        awaitRunning()
        engine.stop()
        awaitStopped()

        engine.start()
        awaitRunning()
        assertTrue(engine.isReady.value)
    }

    @Test
    fun stopRacingStartLeavesALaterStartRunning() = runBlocking {
        engine.start()
        engine.stop()
        engine.start()
        awaitRunning()
        assertTrue(engine.isReady.value)
    }

    private suspend fun awaitRunning() = withTimeout(TIMEOUT_MS) {
        engine.isReady.first { it }
        while (engine.stats().sampleRate == 0) delay(POLL_MS)
    }

    private suspend fun awaitStopped() = withTimeout(TIMEOUT_MS) {
        while (engine.stats().sampleRate != 0) delay(POLL_MS)
    }
}
