package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.plus

import org.team2471.frc.powerup.RobotMap

object Carriage {
    private const val CARRIAGE_SRX_UNITS_TO_INCHES = (1.75 * Math.PI) / 750 // TODO: check for later

    private val motors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    }

    private val position: Double
        get() = motors.getSelectedSensorPosition(0) * CARRIAGE_SRX_UNITS_TO_INCHES

    private var setpoint: Double = position
        set(value) {
            field = value
            motors.set(ControlMode.Position, value / CARRIAGE_SRX_UNITS_TO_INCHES)
        }

    private object Arm {
        private const val ARM_SRX_UNITS_TO_DEGREES = 1.0

        private val pivotMotors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
        } + TalonSRX(RobotMap.Talons.ARM_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
        }

        private val intakeMotorLeft = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            inverted = true
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
        }

        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        val position: Double
            get() = pivotMotors.getSelectedSensorPosition(0) * ARM_SRX_UNITS_TO_DEGREES

        var setpoint: Double = position
            set(value) {
                field = value
                pivotMotors.set(ControlMode.Position, value / ARM_SRX_UNITS_TO_DEGREES)
            }

        var clawClosed: Boolean
            get() = clawSolenoid.get()
            set(value) = clawSolenoid.set(value)
    }

}