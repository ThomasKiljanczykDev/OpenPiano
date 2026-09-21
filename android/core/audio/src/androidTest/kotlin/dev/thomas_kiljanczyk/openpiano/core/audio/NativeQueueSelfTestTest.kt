package dev.thomas_kiljanczyk.openpiano.core.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeQueueSelfTestTest {

    @Test
    fun preservesFifoOrder() {
        assertNull(NativeQueueSelfTest.nativeFifoOrder())
    }

    @Test
    fun dropsExactlyOneWhenFull() {
        assertNull(NativeQueueSelfTest.nativeDropWhenFull())
    }

    @Test
    fun losesNothingUnderConcurrentLoad() {
        assertNull(NativeQueueSelfTest.nativeNoDropUnderLoad())
    }
}
