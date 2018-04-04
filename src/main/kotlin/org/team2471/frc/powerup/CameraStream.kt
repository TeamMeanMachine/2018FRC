package org.team2471.frc.powerup

import edu.wpi.cscore.*
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc.putText
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Pose
import java.awt.Color
import kotlin.concurrent.thread

object CameraStream {
    private val white = Scalar(255.0,255.0,255.0)
    private val red = Scalar(0.0,0.0,255.0)

    init {
        val camera = UsbCamera("Arm Camera", 0)
        camera.setResolution(320, 240)
        camera.setFPS(15)

        val cameraSink = CvSink("Camera Sink")
        cameraSink.source = camera

        val server = MjpegServer("Arm Camera Server", 5805)
        val processedSource = CvSource("Processed Source", VideoMode.PixelFormat.kBGR, 320, 240, 15)
        server.source = processedSource

        thread {
            val image = Mat()
            while (true) {
                cameraSink.grabFrame(image)
                if ((!Carriage.targetPose.isScale || Carriage.targetPose == Pose.SCALE_FRONT) && Carriage.isAnimationCompleted) {
                    Core.rotate(image, image, 1) // Rotate 180 degrees
                }
                val matchTime = Game.matchTime
                putText(image, "$matchTime", Point(300.0,15.0), 2, 1.0,
                        if (matchTime > 45.0) white else red)
                processedSource.putFrame(image)
            }
        }

    }
}