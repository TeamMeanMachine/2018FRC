package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.SerialPort

object LEDController {
    private val port = SerialPort(9600, SerialPort.Port.kUSB2)

    var state: LEDState = IdleState

        set(value) {
            port.writeString("${value.representation}\n")
            field = value
        }
}

sealed class LEDState {
    abstract val representation: String
}


object IdleState : LEDState() {
    override val representation: String
        get() = "randomcrap"
}

object Stop : LEDState(){
    override val representation: String
        get() = "stop"
}

object go : LEDState(){
    override val representation: String
        get() = "go"
}
object fire : LEDState(){
    override val representation: String
        get() = "fire"
}
object the : LEDState(){
    override val representation: String
        get() = "the"
}
object bounce : LEDState(){
    override val representation: String
        get() = "randomcrap"
}



