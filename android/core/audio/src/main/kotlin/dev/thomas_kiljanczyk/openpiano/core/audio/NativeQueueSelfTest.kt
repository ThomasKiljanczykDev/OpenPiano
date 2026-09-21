package dev.thomas_kiljanczyk.openpiano.core.audio

/**
 * Diagnostic surface over the native SPSC ring buffer. Each call returns null when the property
 * holds and a failure description otherwise. The queue is a header-only template with no Kotlin
 * counterpart, so it can only be exercised from native code.
 */
object NativeQueueSelfTest {
    external fun nativeFifoOrder(): String?

    external fun nativeDropWhenFull(): String?

    external fun nativeNoDropUnderLoad(): String?

    init {
        System.loadLibrary("openpiano_audio")
    }
}
