package dev.thomas_kiljanczyk.openpiano.buildlogic

// App and native modules must agree: an ABI packaged without libopenpiano_audio crashes at load.
val SUPPORTED_ABIS = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
