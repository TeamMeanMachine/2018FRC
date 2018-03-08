package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.SerialPort
import org.team2471.frc.lib.util.Alliance

object LEDController {
    private val port = SerialPort(9600, SerialPort.Port.kUSB2)

    var alliance: Alliance? = null
        @Synchronized set(value) {
            if (value == DriverStation.Alliance.Red) {
                port.writeString("red")
            } else if (value == DriverStation.Alliance.Blue) {
                port.writeString("blue")
            }
            field = value
        }

    var state: LEDState? = IdleState
        @Synchronized set(value) {
            if (value != null) port.writeString("${value.representation}\n")
            field = value
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