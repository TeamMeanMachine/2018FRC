package org.team2471.frc.powerup.Subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.RobotMap

object Drive {
    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1) + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2)
    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1) + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2)

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }
        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)
    }

    init {
        CommandSystem.registerDefaultCommand(this, Command("Drive Default", this) {
            periodic {
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }
}