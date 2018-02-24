package org.team2471.powerupvision

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object CameraStream {
    val TAG = "CameraStream"

    private val jpeg = MatOfByte()

    fun updateImage(image: Mat) {
        Imgcodecs.imencode("jpg", image, jpeg)
    }

    private val serverSocket = ServerSocket(5800)

    init {
        thread {
            Log.i(TAG, "Waiting for a connection")
            val socket = serverSocket.accept()
            Log.i(TAG, "Found a socket: " + socket)
            spawnClientThread(socket)
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
            }
        } catch (e: IOException) {
            Log.i(TAG, "Camera Disconnected")
        }

    }
}