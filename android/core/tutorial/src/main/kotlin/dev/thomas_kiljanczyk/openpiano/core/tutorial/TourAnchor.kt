package dev.thomas_kiljanczyk.openpiano.core.tutorial

/** Declared here so `:app` can reference targets without a feature-to-feature dependency. */
enum class TourAnchor {
    KEYBOARD_KEYS,
    KEYBOARD_OVERVIEW,
    KEYBOARD_SHIFT_DOWN,
    KEYBOARD_SHIFT_UP,
    KEYBOARD_LOWEST_NOTE,
    KEYBOARD_SETTINGS,

    SETTINGS_VISIBLE_KEYS,
    SETTINGS_TOUCH_MODE,
    SETTINGS_TOUCH_PRECISION,
    SETTINGS_REPLAY_TUTORIAL,
}
