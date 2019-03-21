package org.team2471.frc.powerup.endgame

import edu.wpi.first.wpilibj.I2C
import java.nio.ByteBuffer

private const val DEVICE_ADDRESS = 0x29
private const val ID_REGISTER = 0x12
private const val DATA_REGISTER = 0x14
private const val INTEGRATION_TIME_REGISTER = 0x01
private const val GAIN_REGISTER = 0x0F

class AdafruitColorSensor(
        port: I2C.Port = I2C.Port.kOnboard,
        integrationTime: IntegrationTime = IntegrationTime.T_24MS,
        gain: Gain = Gain.X1
) {
    private val i2c = I2C(port, DEVICE_ADDRESS)
    private val dataBuffer = ByteBuffer.allocate(8)

    var isInitialized: Boolean = false
        private set

    var integrationTime: IntegrationTime = integrationTime
        set (value) {
            if (!isInitialized) initialize()
            i2c.write(INTEGRATION_TIME_REGISTER, value.id)
            field = value
        }

    var gain: Gain = gain
        set (value) {
            if (!isInitialized) initialize()
            i2c.write(GAIN_REGISTER, value.id)
            field = value
        }

    fun readColor(color: Color) {
        if (!isInitialized) initialize()

        i2c.read(DATA_REGISTER, 8, dataBuffer)

        color.clear = dataBuffer.getShort(0).toInt()
        color.red = dataBuffer.getShort(2).toInt()
        color.green = dataBuffer.getShort(4).toInt()
        color.blue = dataBuffer.getShort(6).toInt()
    }

    private fun initialize(): Boolean {
        i2c.read(ID_REGISTER, 1, dataBuffer)

        val x = dataBuffer.get(0)
        if (x != 0x12.toByte()) {
            println("Failed to initialize color sensor ($x)")
            return false
        }
        isInitialized = true

        integrationTime = integrationTime
        gain = gain

        println("Color sensor initialized")
        return true
    }

    data class Color(var red: Int, var green: Int, var blue: Int, var clear: Int)

    enum class IntegrationTime(internal val id: Int) {
        /**
         * 2.4ms - 1 cycle - Max Count: 1024
         */
        T_2_4MS(0xFF),
        /**
         * 24ms  - 10 cycles  - Max Count: 10240
         */
        T_24MS(0xF6),
        /**
         * 50ms  - 20 cycles  - Max Count: 20480
         */
        T_50MS(0xEB),
        /**
         * 101ms - 42 cycles  - Max Count: 43008
         */
        T_101MS(0xD5),
        /**
         * 154ms - 64 cycles  - Max Count: 65535
         */
        T_154MS(0xC0),
        /**
         * 700ms - 256 cycles - Max Count: 65535
         */
        T_700MS(0x00)
    }

    enum class Gain (internal val id: Int){
        /**
         * No gain
         */
        X1(0x00),
        /**
         * 4x gain
         */
        X4(0x01),
        /**
         * 16x gain
         */
        X16(0x02),
        /**
         * 60x gain
         */
        X60(0x03)
    }
}