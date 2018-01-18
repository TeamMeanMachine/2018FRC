package org.team2471.powerupvision

import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


object VisionProcessing {
    private val hsv = Mat()
    private val thresh = Mat()
    private val hierarchy = Mat()



    private val allContours: MutableList<MatOfPoint> = ArrayList()
    private val filteredContours: MutableList<MatOfPoint> = ArrayList()

    private val hsvMins = Scalar(0.0,0.0,0.0)
    private val hsvMaxes = Scalar(0.0,0.0,0.0)

    private val contourColor = Scalar(255.0,0.0,0.0)


    private var aspect_ratio = 0.0


    fun processImage(inputImage: Mat, displayMode: DisplayMode): Mat {
        //val cnt = contours[0]
        Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV)
        updateHSVThreshhold()
        Core.inRange(hsv, hsvMins, hsvMaxes, thresh)

        Log.i("Aspect Ratio", "$aspect_ratio")
        allContours.clear()
        Imgproc.findContours(thresh, allContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

//        for (i in allContours) {
//            println("yee")
//        }


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