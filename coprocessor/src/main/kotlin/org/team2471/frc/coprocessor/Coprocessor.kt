package org.team2471.frc.coprocessor

import com.atul.JavaOpenCV.Imshow
import cz.adamh.utils.NativeUtils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture

object Coprocessor {
    @JvmStatic
    fun main(args: Array<String>) {
        NativeUtils.loadLibraryFromJar("/windows/x86-64/opencv_java320.dll")

        val raw = Mat()
        val hsv = Mat()
        val thresh = Mat()

        val lowerYellow = Scalar(25.0, 0.0, 0.0)
        val upperYellow = Scalar(40.0, 255.0, 255.0)

        val capture = VideoCapture(2)
        val testingWindow = Imshow("Test Window",640, 480)

        while (true){
            capture.retrieve(raw)
            Imgproc.cvtColor(raw, hsv, Imgproc.COLOR_BGR2HSV_FULL)
            Core.inRange(hsv, lowerYellow, upperYellow, thresh)
            testingWindow.showImage(thresh)
            val centerPoint = hsv.get(320, 240)
            println("Hue: ${centerPoint[0]}, Saturation: ${centerPoint[1]}, Value: ${centerPoint[2]}")
        }
    }
}