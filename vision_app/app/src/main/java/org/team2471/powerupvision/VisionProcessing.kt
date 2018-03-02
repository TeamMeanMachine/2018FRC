package org.team2471.powerupvision

import android.util.AttributeSet
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


object VisionProcessing {
    private const val TAG =  "Vision Processing"

    private val hsv = Mat()
    private val thresh = Mat()
    private val eroded = Mat()
    private val dilated = Mat()

    private val hierarchy = Mat()


    private val allContours: MutableList<MatOfPoint> = ArrayList()
    private val filteredContours: MutableList<MatOfPoint> = ArrayList()

    private val hsvMins = Scalar(0.0,0.0,0.0)
    private val hsvMaxes = Scalar(0.0,0.0,0.0)
    private val erosionKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
            Size(8.0,8.0), Point(4.0,4.0))
    private val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
            Size(12.0,12.0), Point(6.0,6.0))

    private val contourColor = Scalar(255.0,0.0,0.0)

    fun processImage(inputImage: Mat): Mat {
        Log.d(TAG, "Image received")
        val displayMode = ImagePreferences.displayMode
        //Imgproc.GaussianBlur(inputImage, blur, gaussianSize, 0.0)
        Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_RGB2HSV)
        updateHSVThreshhold()
        Core.inRange(hsv, hsvMins, hsvMaxes, thresh)

        Imgproc.erode(thresh, eroded, erosionKernel)
        Imgproc.dilate(eroded,dilated, dilateKernel)

        allContours.clear()
        filteredContours.clear()
        Imgproc.findContours(if( displayMode == DisplayMode.THRESH_DEBUG) dilated.clone()
            else dilated, allContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_KCOS)

        allContours.forEachIndexed { index, contour ->
            val boundingBox = Imgproc.boundingRect(contour)
            val area = boundingBox.area()
            if (area < 9000 || area > 900000) return@forEachIndexed
            val aspectRatio = boundingBox.width.toDouble() / boundingBox.height
            if (aspectRatio < .7 || aspectRatio > 1.5) return@forEachIndexed
            Log.d(TAG, "Index: $index, Aspect Ratio: $aspectRatio, Area: $area")
            filteredContours.add(allContours[index])

        }

        //filteredContour

        return when(displayMode) {
            VisionProcessing.DisplayMode.RAW -> inputImage
            VisionProcessing.DisplayMode.THRESH -> thresh.apply {
                Imgproc.cvtColor(this, this, Imgproc.COLOR_GRAY2BGR)
            }
            VisionProcessing.DisplayMode.THRESH_DEBUG -> dilated.apply {
                Imgproc.cvtColor(this, this, Imgproc.COLOR_GRAY2BGR)
            }
        }.apply {
            Imgproc.drawContours(this, filteredContours, -1, contourColor, 2)
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
        THRESH_DEBUG
    }
}