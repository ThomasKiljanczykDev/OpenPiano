package dev.thomas_kiljanczyk.openpiano.core.tutorial

const val CURRENT_TUTORIAL_VERSION = 1

/** Replay stores 0, which is below every version. */
fun shouldShowTutorial(completedVersion: Int): Boolean = completedVersion < CURRENT_TUTORIAL_VERSION
