package org.team2471.frc.powerup

import com.team254.frc2017.vision.AdbBridge
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object AndroidPhone {
    private val adb = AdbBridge()

    init {
        adb.start()
        setup()
        launchCameraServerThread()
    }

    private fun setup() {
        adb.portForward(2471, 2471)
    }

    private fun launchCameraServerThread() {
        thread {
            println("Starting server on port 5804")
            val serverSocket = ServerSocket(5804)
            while (true) {
                launchCameraClientThread(serverSocket.accept())
            }
        }
    }

    private fun launchCameraClientThread(socket: Socket) {
        thread {
            val clientInput = socket.getInputStream()
            val clientOutput = socket.getOutputStream()

            println("Client found at ${socket.remoteSocketAddress}")
            val phoneConnection = Socket("localhost", 2471)

            val serverInput = phoneConnection.getInputStream()
            val serverOutput = phoneConnection.getOutputStream()

            val clientBuffer = ByteArray(8192)
            val serverBuffer = ByteArray(8192)

            while (true) {
                println("Start reading client")
                val clientRead = clientInput.read(clientBuffer)
                println("Done reading client")

                print("Client: ${String(clientBuffer, 0, clientRead)}")
                serverOutput.write(clientBuffer, 0, clientRead)

                println("Start reading server")
                println("Server available: ${serverInput.available()}")
                val serverRead = serverInput.read(serverBuffer)
                println("Done reading server")

                print("Server: ${String(serverBuffer, 0, serverRead)}")
                clientOutput.write(serverBuffer, 0, serverRead)
            }
        }
    }
}