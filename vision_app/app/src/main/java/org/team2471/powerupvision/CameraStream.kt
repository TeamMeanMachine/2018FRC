package org.team2471.powerupvision

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object CameraStream {
    private const val TAG = "CameraStream"
    private const val FPS = 15L

    private val image = Mat()

    private val jpeg = MatOfByte()

    fun updateImage(image: Mat) {
        Imgproc.cvtColor(image, this.image, Imgproc.COLOR_RGB2BGR)
    }

    private val serverSocket = ServerSocket(2471)

    init {
        thread {
            while (true) {
                Log.i(TAG, "Waiting for a connection")
                val socket = serverSocket.accept()
                Log.i(TAG, "Found a socket: " + socket)
                spawnClientThread(socket)
            }
        }
    }

    private fun spawnClientThread(socket: Socket) = thread {
        try {
            Log.i(TAG, "Starting a connection")
            val stream = socket.getOutputStream()
            stream.write((
                    "HTTP/1.0 200 OK\r\n" +
                            "Server: Android Vision Stream\r\n" +
                            "Connection: close\r\n" +
                            "Max-Age: 0\r\n" +
                            "Expires: 0\r\n" +
                            "Cache-Control: no-cache, private\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: multipart/x-mixed-replace; " +
                            "boundary=--BoundaryString\r\n\r\n").toByteArray())
            while (true) {
                Imgcodecs.imencode(".jpg", this.image, jpeg)
                val data = jpeg.toArray()
                stream.write((
                        "--BoundaryString\r\n" +
                                "Content-type: image/jpg\r\n" +
                                "Content-Length: " +
                                data.size +
                                "\r\n\r\n").toByteArray())
                stream.write(data)
                stream.write("\r\n\r\n".toByteArray())
                stream.flush()
                Thread.sleep(1/(1000 * FPS))
            }
        } catch (e: IOException) {
            Log.i(TAG, "Camera Disconnected")
        }

    }
}