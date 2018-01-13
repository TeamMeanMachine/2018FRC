package org.team2471.frc.coprocessor

import com.atul.JavaOpenCV.Imshow
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture

object Coprocessor {
    @JvmStatic
    fun main(args: Array<String>) {
//        System.load("windows/x86-64/opencv_java320.dll")
        val mat = Mat()
        val capture = VideoCapture(0)
        while (true){
            capture.retrieve(mat)
            Imshow.show(mat)
        }
    }
}