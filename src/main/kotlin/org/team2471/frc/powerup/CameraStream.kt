package org.team2471.frc.powerup

import edu.wpi.cscore.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.putText
import org.team2471.frc.powerup.carriage.Arm
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Pose
import java.awt.Color
import kotlin.concurrent.thread

object CameraStream {
    private const val WIDTH = 320
    private const val HEIGHT = 240
    private const val FPS = 15
    private const val FREEZEFRAME_WIDTH = WIDTH / 4
    private const val FREEZEFRAME_HEIGHT = HEIGHT / 4

    private val white = Scalar(255.0,255.0,255.0)
    private val red = Scalar(0.0,0.0,255.0)
    private val black = Scalar(0.0,0.0,0.0)

    init {
        val camera = UsbCamera("Arm Camera", 0)
        camera.setResolution(WIDTH, HEIGHT)
        camera.setFPS(FPS)

//        val cameraSink = CvSink("Camera Sink")
//        cameraSink.source = camera

        val server = MjpegServer("Arm Camera Server", 5805)
//        val processedSource = CvSource("Processed Source", VideoMode.PixelFormat.kBGR, WIDTH + WIDTH/4, HEIGHT, FPS)
        server.source = camera

        /*
        thread {
            val image = Mat()
            val info = Mat(FREEZEFRAME_WIDTH, HEIGHT - FREEZEFRAME_HEIGHT, CvType.CV_8U)
            val freezeFrame = Mat(FREEZEFRAME_WIDTH, FREEZEFRAME_HEIGHT, CvType.CV_8U)
            val sidebar = Mat(FREEZEFRAME_WIDTH, HEIGHT, CvType.CV_8U)
            val processedImage = Mat()

            var prevIsScale = Carriage.targetPose.isScale
            while (true) {
                cameraSink.grabFrame(image)
                if (Arm.angle < 100.0) {
                    Core.rotate(image, image, 1) // Rotate 180 degrees
                }
                Imgproc.line(image, Point(WIDTH/2.0, 0.0), Point(WIDTH/2.0, HEIGHT.toDouble()), red, 1)

                val matchTime = Game.matchTime
                putText(info, "$matchTime", Point(5.0,5.0), 2, 0.2,
                        if (matchTime > 45.0) white else red)
                Imgproc.rectangle(info, Point(0.0,0.0), Point(info.width().toDouble(), info.height().toDouble()), black)
                val isScale = Carriage.targetPose.isScale
                if (prevIsScale && !isScale) {
                    Imgproc.resize(image, freezeFrame, Size(FREEZEFRAME_WIDTH.toDouble(), FREEZEFRAME_HEIGHT.toDouble()))
                }

//                println(image.type())
//                println("info: ${info.size()}, freezeFrame: ${freezeFrame.size()}")
//                Core.vconcat(listOf(info, freezeFrame), sidebar)
//                Core.hconcat(listOf(sidebar, image), processedImage)

                processedSource.putFrame(image)
                prevIsScale = isScale
            }
        }*/
    }
}