package org.team2471.powerupvision

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import org.opencv.android.JavaCameraView

class CameraView(context: Context, attrs: AttributeSet) : JavaCameraView(context, attrs) {
    private val TAG = "Camera View"

    override fun onPreviewFrame(frame: ByteArray?, arg1: Camera?) {
        super.onPreviewFrame(frame, arg1)
        val parameters = mCamera.parameters
        parameters.exposureCompensation = ImagePreferences.exposure
        mCamera.parameters = parameters
    }
}