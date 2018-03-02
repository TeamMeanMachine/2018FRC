package org.team2471.powerupvision

import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker

sealed class Message {
    abstract val bytes: ByteArray

    companion object {
        fun parse(unpacker: MessageUnpacker): Message? {
            val type = unpacker.unpackByte()

            return try {
                when (type) {
                    0.toByte() -> HeartbeatMessage
                    1.toByte() -> SetCameraModeMessage(CameraMode.valueOf(unpacker.unpackString()))
                    2.toByte() -> CameraUpdateMessage(Array(unpacker.unpackArrayHeader()) {
                        Target(unpacker.unpackDouble(), unpacker.unpackDouble())
                    })
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

object HeartbeatMessage : Message() {
    override val bytes: ByteArray = byteArrayOf(0)
}

class SetCameraModeMessage(private val mode: CameraMode) : Message() {
    override val bytes: ByteArray
        get() {
            val packer = MessagePack.newDefaultBufferPacker()
            packer.packByte(1)
            packer.packString(mode.name)
            return packer.toByteArray()
        }
}

class CameraUpdateMessage(private val targets: Array<Target>) : Message() {
    override val bytes: ByteArray
        get() {
            val packer = MessagePack.newDefaultBufferPacker()
            packer.packByte(2)
            packer.packArrayHeader(targets.size)
            targets.forEach { target ->
                packer.packDouble(target.angle)
                packer.packDouble(target.distance)
            }
            return packer.toByteArray()
        }
}

data class Target(val angle: Double, val distance: Double)

enum class CameraMode {
    IDLE,
    FIND_CUBE,
}