package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.RobotMap.Solenoids.ARM_CLAMP
import org.team2471.frc.powerup.RobotMap.Talons.INTAKE_MOTOR_LEFT
import org.team2471.frc.powerup.RobotMap.Talons.INTAKE_MOTOR_RIGHT

object Intake {
    private val armClamp = Solenoid(ARM_CLAMP)
    private val leftMotor = TalonSRX(INTAKE_MOTOR_LEFT).apply {
        configContinuousCurrentLimit(10, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(100, 10)
        enableCurrentLimit(true)
    }
    private val rightMotor= TalonSRX(INTAKE_MOTOR_RIGHT).apply {
        configContinuousCurrentLimit(10, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(100, 10)
        enableCurrentLimit(true)
    }

    private var leftSpeed: Double
        get() = leftMotor.motorOutputVoltage/12
        set(value) = leftMotor.set(ControlMode.PercentOutput, value)

    private var rightSpeed: Double
        get() = rightMotor.motorOutputVoltage/12
        set(value) = rightMotor.set(ControlMode.PercentOutput, value)

    private var clamp: Boolean = false
        set(value) {
            field = value
            armClamp.set(clamp)
        }


    val toggleClampCommand = Command("Toggle Clamp") {
        clamp = !clamp

    }


    init {
        CommandSystem.registerDefaultCommand(this, Command("Intake Default", this) {
            periodic {
                if (CoDriver.invertIntake) {
                    leftSpeed = -CoDriver.leftIntake
                    rightSpeed = -CoDriver.rightIntake
                } else {
                    leftSpeed = CoDriver.leftIntake
                    rightSpeed = CoDriver.rightIntake
                }
            }
        })
    }
}