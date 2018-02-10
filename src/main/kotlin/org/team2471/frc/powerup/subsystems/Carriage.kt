package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.math.average

import org.team2471.frc.powerup.RobotMap
import kotlin.math.absoluteValue

object Carriage {
    private const val CARRIAGE_SRX_UNITS_TO_INCHES = (1.75 * Math.PI) / 750 // TODO: check for later

    private val motors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
    }

    private val position: Double
        get() = motors.getSelectedSensorPosition(0) * CARRIAGE_SRX_UNITS_TO_INCHES

    private var setpoint: Double = position
        set(value) {
            field = value
            motors.set(ControlMode.Position, value / CARRIAGE_SRX_UNITS_TO_INCHES)
        }

    var diskBrake = Solenoid(RobotMap.Solenoids.BRAKE)

    var isLowGear: Boolean
        get() = diskBrake.get()
        set(value) = diskBrake.set(isLowGear)


    enum class ShiftSetting {
        FORCE_HIGH,
        FORCE_LOW
    }

    suspend fun moveToPose(pose: Pose){

    }

    class Pose(val inches: Double, val armAngle: Double) {
        companion object {
            val IDLE = Pose(0.0, 90.0)
            val INTAKE_POS = Pose(0.0, 0.0)
            val SCALE_POS = Pose(60.0, 180.0)
            val SWITCH_POS = Pose(20.0, 15.0)
            val SCALE_SAFETY = Pose(60.0, 90.0)
            val CLIMB = Pose(40.0, 0.0)
        }
    }

    private val currentPose get() = Pose(Arm.height, Arm.armAngle)

    object Arm {
        private const val ARM_SRX_UNITS_TO_DEGREES = 1.0
        private val offset: Double = 170.0

        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

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
            inverted = true
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        var clamp: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        var armAngle: Double
            get() = nativeUnitsToDegrees(pivotMotors.activeTrajectoryPosition)
            set(value) {
                setpoint = degreesToNativeUnits(value)
            }

        var height: Double
            get() = ticksToInches(motors.activeTrajectoryPosition)
            set(value) {
                Carriage.setpoint = ticksToInches(value.toInt())
            }

        var intake: Double
            get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
            set(speed) {
                intakeMotorLeft.set(ControlMode.PercentOutput, speed)
                intakeMotorRight.set(ControlMode.PercentOutput, speed)
            }

        private const val TICKS_PER_REV = 783
        private const val SPOOL_DIAMETER_INCHES = 2.0
        private fun ticksToInches(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * SPOOL_DIAMETER_INCHES * Math.PI
        private fun nativeUnitsToDegrees(nativeUnits: Int): Double = (nativeUnits - offset) / (8.0 / 3.0)
        private fun degreesToNativeUnits(angle: Double): Double = (angle) * (8.0 / 3.0) + offset

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