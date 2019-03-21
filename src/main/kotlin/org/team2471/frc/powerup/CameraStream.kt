package org.team2471.frc.powerup

import edu.wpi.cscore.UsbCamera

object CameraStream {
    private const val WIDTH = 320
    private const val HEIGHT = 240
    private const val FPS = 15

    //private val server = MjpegServer("Arm Camera Server", 5805)

//    val camera = UsbCamera("Arm Camera", 0).apply {
//        setResolution(WIDTH, HEIGHT)
//        setFPS(FPS)
//    }

    var isEnabled = false
        set(value) {
//            field = value
//            server.source = if (value) {
//                camera
//            } else {
//                null
//            }
        }
}