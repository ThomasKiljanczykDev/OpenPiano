package dev.thomas_kiljanczyk.openpiano.core.data.touch

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidTouchAreaSupport @Inject constructor(@ApplicationContext context: Context) : TouchAreaSupport {

    override val isSupported: Boolean = run {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        val deviceIds = inputManager?.inputDeviceIds ?: IntArray(0)
        deviceIds.any { id ->
            val device = inputManager?.getInputDevice(id)
            device != null &&
                device.supportsSource(InputDevice.SOURCE_TOUCHSCREEN) &&
                device.getMotionRange(MotionEvent.AXIS_TOUCH_MAJOR) != null
        }
    }
}
