package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.SerialPort
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.util.Alliance

object LEDController {
    private var port: SerialPort? = null

    init {
        launch {
            while(port == null) {
                try {
                    port = SerialPort(9600, SerialPort.Port.kUSB1).also {
                        println("LEDController found")
                    }
                } catch (e: Exception) {
                    DriverStation.reportError("Failed to connect to LEDController\n${e.message}. Trying again in 5 seconds.", false)
                    delay(5000)
                }
            }
        }
    }

    var alliance: Alliance? = null
        @Synchronized set(value) {
            println("Alliance $value received. Previous $alliance")
            if (value != field) {
                if (value == Alliance.RED) {
                    write("red")
                } else if (value == Alliance.BLUE) {
                    write("blue")
                }
            }
            field = value
        }

    var state: LEDState? = IdleState
        @Synchronized set(value) {
            if (value != field && value != null) {
                write(value.representation)
            }
            field = value
        }

    private fun write(data: String) = try {
        println("sending $data to LedController")
        port?.writeString("$data\n")
    } catch (e: Exception) {
        DriverStation.reportError("Error writing string to LEDController: ${e.message}", false)
    }
}


sealed class LEDState {
    abstract val representation: String
}

object IdleState : LEDState() {
    override val representation: String
        get() = "idle"
}

object ClimbStopState : LEDState() {
    override val representation: String
        get() = "stop"
}

object ClimbGoState : LEDState() {
    override val representation: String
        get() = "go"
}

class ClimbingState(
        private val speed: Byte // between -100 and 100 (both inclusive)
) : LEDState() {
    override val representation: String
        get() = "up-$speed"
}

object FireState : LEDState() {
    override val representation: String
        get() = "fire"
}

object TheatreState : LEDState() {
    override val representation: String
        get() = "the"
}

object BounceState : LEDState() {
    override val representation: String
        get() = "bounce"
}

object CallibrateGyroState: LEDState() {
    override val representation: String
        get() = "white"
}