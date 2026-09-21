package dev.thomas_kiljanczyk.openpiano.core.testing

import dev.thomas_kiljanczyk.openpiano.core.data.touch.TouchAreaSupport

class FakeTouchAreaSupport(override val isSupported: Boolean = true) : TouchAreaSupport
