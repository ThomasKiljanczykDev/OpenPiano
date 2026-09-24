package dev.thomas_kiljanczyk.openpiano.buildlogic

object AppVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val PATCH = 1

    /*
     * MAJOR: 0–2099 (2100 versions; Play caps versionCode at 2_100_000_000)
     * MINOR: 0–999 (1000 versions per major)
     * PATCH: 0–999 (1000 versions per minor)
     */
    const val versionCode: Int = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH
    const val versionName: String = "$MAJOR.$MINOR.$PATCH"
}
