package org.team2471.frc.powerup.vision

import org.msgpack.core.MessagePack
import java.io.OutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

object VisionServer {
    private const val PORT = 2471
    private val serverSocket = ServerSocket(PORT)
    private var outputStream: OutputStream? = null

    init {
        ADB.start()
        ADB.reversePortForward(PORT, PORT)

        thread {
            val unpackerConfig = MessagePack.UnpackerConfig().withBufferSize(8192)

            while (true) {
                println("Waiting for Android Device")
                val socket = serverSocket.accept()
                outputStream = socket.getOutputStream()
                val unpacker = unpackerConfig.newUnpacker(socket.getInputStream())
                while (socket.isConnected) {
                    while (unpacker.hasNext()) {
                        val message = Message.parse(unpacker) ?: continue
                        handleMessage(message)
                    }
                }
                outputStream = null
            }
        }
    }

    private fun handleMessage(message: Message) {

    }

    fun sendMessage(message: Message) {
        outputStream?.write(message.bytes)
    }
}


