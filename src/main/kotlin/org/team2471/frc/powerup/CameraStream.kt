package org.team2471.frc.powerup

import edu.wpi.cscore.MjpegServer
import edu.wpi.cscore.UsbCamera
import edu.wpi.cscore.VideoMode
import edu.wpi.first.wpilibj.SerialPort
import kotlinx.coroutines.experimental.launch
import kotlin.concurrent.thread

object CameraStream {
    private const val WIDTH = 320
    private const val HEIGHT = 240
    private const val FPS = 30

    private val server = MjpegServer("Camera Server", 5805)

    private val camera = UsbCamera("Camera", 0).apply {
        setResolution(WIDTH, HEIGHT)
        setFPS(FPS)
        setPixelFormat(VideoMode.PixelFormat.kYUYV)
    }

    var isEnabled = false
        set(value) {
            field = value
            server.source = if (value) {
                camera
            } else {
                null
            }
        }

    init {
        thread {
            println("Opening serial port")
            val serial = SerialPort(9600, SerialPort.Port.kUSB)
            serial.writeString("setpar serout USB\n")

            println("Reading")
            while(true) {
                print(serial.readString())
            }
        }
    }
}