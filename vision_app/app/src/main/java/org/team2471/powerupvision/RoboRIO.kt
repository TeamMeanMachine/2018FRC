package org.team2471.powerupvision

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.hoho.android.usbserial.driver.UsbSerialProber

class RoboRIO(context: Context) {
    val TAG = "RoboRio"
    init {
        val usbManager = context.getSystemService(UsbManager::class.java)

        val fpsTextView = (context as Activity).findViewById<TextView>(R.id.fps_text_view)

        Handler(Looper.getMainLooper()).post {
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                Log.i(TAG, "No Devices Found")
                return@post
            }

            val driver = availableDrivers.first()
            val connection = usbManager.openDevice(driver.device) ?: run {
                Log.e(TAG, "Connection to device cannot be established")
                return@post
            }
            val port = driver.ports.first()

            fpsTextView.setText("Device Found")
        }

    }
}