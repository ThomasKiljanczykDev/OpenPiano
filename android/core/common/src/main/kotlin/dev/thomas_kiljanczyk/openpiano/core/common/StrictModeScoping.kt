package dev.thomas_kiljanczyk.openpiano.core.common

import android.os.StrictMode

/** Suspends StrictMode disk-read detection for [block], restoring the prior thread policy after. */
inline fun <T> allowingThreadDiskReads(block: () -> T): T {
    val previousPolicy = StrictMode.allowThreadDiskReads()
    try {
        return block()
    } finally {
        StrictMode.setThreadPolicy(previousPolicy)
    }
}
