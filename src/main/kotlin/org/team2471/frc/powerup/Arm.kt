package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus

object Arm {
    private val shoulderMotors = TalonSRX(RobotMap.Talons.ARM_SHOULDER_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
        setNeutralMode(NeutralMode.Brake)

        inverted = true
    } + TalonSRX(RobotMap.Talons.ARM_SHOULDER_MOTOR_2)

    private var pivot: Double
        get() = shoulderMotors.motorOutputVoltage
        set(value) = shoulderMotors.set(ControlMode.PercentOutput, value)

    val position get() = shoulderMotors.sensorCollection.analogInRaw

    init {
        CommandSystem.registerDefaultCommand(this, Command("Move Arm", this) {
            periodic {
                var pivot = CoDriver.wristPivot
                if (position >= 440 && pivot < 0 || position <= 170 && pivot > 0) {
                    pivot = 0.0
                }

                this@Arm.pivot = pivot
            }
        })
    }
}