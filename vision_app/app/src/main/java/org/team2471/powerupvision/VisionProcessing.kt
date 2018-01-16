package org.team2471.powerupvision

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


object VisionProcessing {
    private val hsv = Mat()
    private val thresh = Mat()
    private val hierarchy = Mat()

    private val contours:MutableList<MatOfPoint> = ArrayList()

    private val hsvMins = Scalar(0.0,0.0,0.0)
    private val hsvMaxes = Scalar(0.0,0.0,0.0)

    private val contourColor = Scalar(255.0,0.0,0.0)

    fun processImage(inputImage: Mat, displayMode: DisplayMode): Mat {
        Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV)
        updateHSVThreshhold()
        Core.inRange(hsv, hsvMins, hsvMaxes, thresh)

        contours.clear()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        return when(displayMode) {
            VisionProcessing.DisplayMode.RAW -> inputImage.apply {
                Imgproc.drawContours(this, contours, -1, contourColor, 2)
            }
            VisionProcessing.DisplayMode.THRESH -> thresh
        }
    }

    private fun updateHSVThreshhold() {
        hsvMins.`val`[0] = ImagePreferences.hueMin.toDouble()
        hsvMins.`val`[1] = ImagePreferences.satMin.toDouble()
        hsvMins.`val`[2] = ImagePreferences.valMin.toDouble()
        hsvMaxes.`val`[0] = ImagePreferences.hueMax.toDouble()
        hsvMaxes.`val`[1] = ImagePreferences.satMax.toDouble()
        hsvMaxes.`val`[2] = ImagePreferences.valMax.toDouble()
    }

    enum class DisplayMode {
        RAW,
        THRESH,
    }
}