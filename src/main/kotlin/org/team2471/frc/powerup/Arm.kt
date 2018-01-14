package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.plus

object Arm {
    private val shoulderMotors = TalonSRX(RobotMap.Talons.ARM_SHOULDER_MOTOR_1).apply {
        inverted = true
    } + TalonSRX(RobotMap.Talons.ARM_SHOULDER_MOTOR_2)

    private var pivot:Double
        get() = shoulderMotors.motorOutputVoltage
        set(value) = shoulderMotors.set(ControlMode.PercentOutput, value)

    init {
        CommandSystem.registerDefaultCommand(this, Command("Move Arm", this){
            pivot = CoDriver.wristPivot
        })
    }

}