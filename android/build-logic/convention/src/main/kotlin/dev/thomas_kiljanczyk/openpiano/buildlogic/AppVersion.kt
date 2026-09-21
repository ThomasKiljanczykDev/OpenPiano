package dev.thomas_kiljanczyk.openpiano.buildlogic

object AppVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val PATCH = 0

    const val versionCode: Int = MAJOR * 10_000 + MINOR * 100 + PATCH
    const val versionName: String = "$MAJOR.$MINOR.$PATCH"
}
