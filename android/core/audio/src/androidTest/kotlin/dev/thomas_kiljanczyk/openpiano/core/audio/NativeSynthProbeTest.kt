package dev.thomas_kiljanczyk.openpiano.core.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val SOUND_FONT_ASSET = "piano.sf3"
private const val AUDIBLE_PEAK = 0.01f

// -60 dBFS; CC 120 still runs TSF's 10 ms fast release, so the tail is near-silent, not exactly zero.
private const val SILENT_PEAK = 0.001f
private const val FULL_SCALE = 1.0f
private const val LIMITER_THRESHOLD_LINEAR = 0.80f

// The limiter's 3 ms attack lets a cluster onset overshoot the threshold before the envelope
// catches up. Unlimited, this cluster hits the hard clamp in SynthEngine::render and reads 1.0.
private const val LIMITER_CEILING = 0.95f
private const val SUSTAIN_BURSTS = 25
private const val RELEASE_BURSTS = 25
private const val CC_ALL_SOUND_OFF = 120
private const val CC_ALL_NOTES_OFF = 123
private const val SETTLE_BURSTS = 2
private const val MEASURE_BURSTS = 10

@RunWith(AndroidJUnit4::class)
class NativeSynthProbeTest {

    private val soundFont: ByteArray by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open(SOUND_FONT_ASSET)
            .use { it.readBytes() }
    }

    @Test
    fun everyProbedNoteSounds() {
        listOf(48, 60, 84).forEach { note ->
            val peak = NativeSynthProbe.nativePeakAmplitude(soundFont, intArrayOf(note), reverbEnabled = true)
            assertTrue("note $note rendered peak $peak", peak > AUDIBLE_PEAK)
        }
    }

    @Test
    fun rendersSilenceWithoutANoteOn() {
        val peak = NativeSynthProbe.nativePeakAmplitude(soundFont, intArrayOf(), reverbEnabled = true)
        assertTrue("peak $peak after CC 120 should be below $SILENT_PEAK", peak < SILENT_PEAK)
    }

    // At unity gain this cluster peaked at 2.23, clipping every sample over 1.0.
    @Test
    fun aDenseClusterStaysInsideFullScale() {
        val cluster = (48..72).toList().toIntArray()
        val peak = NativeSynthProbe.nativePeakAmplitude(soundFont, cluster, reverbEnabled = true)
        assertTrue("cluster rendered peak $peak", peak > AUDIBLE_PEAK)
        assertTrue("cluster rendered peak $peak should be limited", peak <= LIMITER_CEILING)
        assertTrue("cluster rendered peak $peak", peak < FULL_SCALE)
    }

    @Test
    fun aQuietNoteIsUnaffectedByTheLimiter() {
        val peak = NativeSynthProbe.nativePeakAmplitude(soundFont, intArrayOf(60), reverbEnabled = true)
        assertTrue(
            "quiet note peak $peak should stay well below the limiter threshold",
            peak < LIMITER_THRESHOLD_LINEAR,
        )
        assertTrue("quiet note peak $peak", peak > AUDIBLE_PEAK)
    }

    @Test
    fun reverbAddsATailAfterNoteOff() {
        val notes = intArrayOf(60)
        val peakWithReverb = NativeSynthProbe.nativeReleasePeakAmplitude(
            soundFont,
            notes,
            SUSTAIN_BURSTS,
            RELEASE_BURSTS,
            reverbEnabled = true,
        )
        val peakWithoutReverb = NativeSynthProbe.nativeReleasePeakAmplitude(
            soundFont,
            notes,
            SUSTAIN_BURSTS,
            RELEASE_BURSTS,
            reverbEnabled = false,
        )
        assertTrue(
            "reverb peak $peakWithReverb should exceed dry peak $peakWithoutReverb after note-off",
            peakWithReverb > peakWithoutReverb,
        )
        assertTrue("reverb tail peak $peakWithReverb should be audible", peakWithReverb > AUDIBLE_PEAK)
    }

    @Test
    fun allNotesOffLetsVoicesRingOutTheirRelease() {
        val peak = NativeSynthProbe.nativeControlChangePeakAmplitude(
            soundFont,
            intArrayOf(60),
            CC_ALL_NOTES_OFF,
            SUSTAIN_BURSTS,
            SETTLE_BURSTS,
            MEASURE_BURSTS,
        )
        assertTrue("release tail peak $peak after CC 123 should be audible", peak > AUDIBLE_PEAK)
    }

    @Test
    fun allSoundOffCutsVoicesToSilence() {
        val peak = NativeSynthProbe.nativeControlChangePeakAmplitude(
            soundFont,
            intArrayOf(48, 60, 72),
            CC_ALL_SOUND_OFF,
            SUSTAIN_BURSTS,
            SETTLE_BURSTS,
            MEASURE_BURSTS,
        )
        assertTrue("peak $peak after CC 120 should be below $SILENT_PEAK", peak < SILENT_PEAK)
    }
}
