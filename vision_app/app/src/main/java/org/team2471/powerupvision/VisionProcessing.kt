package org.team2471.powerupvision

import android.util.AttributeSet
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


object VisionProcessing {
    private const val TAG =  "Vision Processing"

    private val blur = Mat()
    private val hsv = Mat()
    private val thresh = Mat()
    private val hierarchy = Mat()


    private val allContours: MutableList<MatOfPoint> = ArrayList()
    private val filteredContours: MutableList<MatOfPoint> = ArrayList()

    private val hsvMins = Scalar(0.0,0.0,0.0)
    private val hsvMaxes = Scalar(0.0,0.0,0.0)
    private val gaussianSize = Size(9.0,9.0)

    private val contourColor = Scalar(255.0,0.0,0.0)

    fun processImage(inputImage: Mat, displayMode: DisplayMode): Mat {
        //Imgproc.GaussianBlur(inputImage, blur, gaussianSize, 0.0)
        Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV)
        updateHSVThreshhold()
        Core.inRange(hsv, hsvMins, hsvMaxes, thresh)


        allContours.clear()
        Imgproc.findContours(thresh, allContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        allContours.forEachIndexed { index, contour ->
            val boundingBox = Imgproc.boundingRect(contour)
            val area = boundingBox.area()
            if (area < 50) return@forEachIndexed

            val aspectRatio = boundingBox.width.toDouble() / boundingBox.height
            Log.d(TAG, "Index: $index, Aspect Ratio: $aspectRatio, Area: $area")
        }

        return when(displayMode) {
            VisionProcessing.DisplayMode.RAW -> inputImage.apply {
                //cnt = contours[0]
                Imgproc.drawContours(this, allContours, -1, contourColor, 2)

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