package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus

object Drive {
    private val shifter = Solenoid(0)

    private val leftMotors = TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_1).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_4).apply {
        setNeutralMode(NeutralMode.Brake)
    }

    private val rightMotors = TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_1).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_4).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    }

    init {
        CommandSystem.registerDefaultCommand(this, Command("Drive Default", this) {
            periodic {
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        shifter.set(true)

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)
    }
}
