package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic

object Elevator {
    private val motor = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR)

    var speed: Double = 0.0
        set(value) {
            field = value
            motor.set(ControlMode.PercentOutput, value)
        }

    init {
        CommandSystem.registerDefaultCommand(this, Command("Elevator Default", this) {
            periodic {
                speed = CoDriver.updown
            }
        })
    }
}