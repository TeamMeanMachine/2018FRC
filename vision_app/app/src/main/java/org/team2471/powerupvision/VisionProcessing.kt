package org.team2471.powerupvision

import android.content.Context
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext

class VisionProcessing(context: Context) {
    init {
        launch(newSingleThreadContext("Vision")) {
            val cameraManager = context.getSystemService(CameraManager::class.java)
            try {
                cameraManager.openCamera("0", cameraCallback, cameraHandler)
            } catch (e:SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private val cameraCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice?) {
        }

        override fun onError(p0: CameraDevice?, p1: Int) {
        }

        override fun onDisconnected(p0: CameraDevice?) {

        }

    }

    private val cameraHandler = Handler()
}