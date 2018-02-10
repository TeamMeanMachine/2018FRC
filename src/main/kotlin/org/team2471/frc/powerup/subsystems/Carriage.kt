package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.DoubleRange
import org.team2471.frc.lib.math.average
import org.team2471.frc.powerup.CoDriver

import org.team2471.frc.powerup.RobotMap
import javax.naming.ldap.Control

object Carriage {
    private const val CARRIAGE_SRX_UNITS_TO_INCHES = (1.75 * Math.PI) / 750 // TODO: check for later

    private const val TICKS_PER_INCH = 7550.0 / 64.25

    private val liftMotors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setSelectedSensorPosition(0, 0, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        configPeakOutputForward(2.4, 10)
        configPeakOutputReverse(-2.4, 10)
        inverted = true
        setSensorPhase(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    }

    private val discBrake = Solenoid(RobotMap.Solenoids.BRAKE)

    private val shifter = Solenoid(RobotMap.Solenoids.CARRIAGE_SHIFT)

    private val position: Double
        get() = liftMotors.getSelectedSensorPosition(0) * CARRIAGE_SRX_UNITS_TO_INCHES

    private var setpoint: Double = position
        set(value) {
            field = value
            liftMotors.set(ControlMode.Position, value / CARRIAGE_SRX_UNITS_TO_INCHES)
        }

    var height: Double
        get() = ticksToInches(liftMotors.activeTrajectoryPosition)
        set(value) {
            Carriage.setpoint = ticksToInches(value.toInt())
        }

    var isLowGear: Boolean
        get() = shifter.get()
        set(value) = shifter.set(value)

    var isBraking: Boolean
        get() = !discBrake.get()
        set(value) = discBrake.set(!value)

    init {
        CommandSystem.registerDefaultCommand(this, Command("Carriage Default", this) {
            periodic {
                //                Arm.setpoint = CoDriver.wristPivot * 90.0 + 90.0
                liftMotors.set(ControlMode.PercentOutput, CoDriver.updown * 0.8)
                println("${liftMotors.getSelectedSensorPosition(0)} -> $height")
                isBraking = CoDriver.brake
                isLowGear = CoDriver.shift
            }
        })
    }

    private const val TICKS_PER_REV = 783
    private const val SPOOL_DIAMETER_INCHES = 2.0
    private fun ticksToInches(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * SPOOL_DIAMETER_INCHES * Math.PI




    suspend fun moveToPose(pose: Pose){

    }

    private fun armRange(height: Double): DoubleRange = when (height) {
        in 0..6 -> 30.0..110.0
        in 6..12 -> 0.0..110.0
        in 12..24 -> 0.0..130.0
        in 24..64 -> 0.0..180.0
        else -> 30.0..110.0
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

    private val currentPose get() = Pose(height, Arm.angle)

    object Arm {
        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        val pivotMotors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            config_kP(0, 0.001, 10)
            config_kI(0, 0.0, 10)
            config_kD(0, 0.0, 10)
            config_kF(0, 0.0, 10)

            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
            setSensorPhase(true)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ARM_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
            inverted = true
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

        val cubeSensor = AnalogInput(3)

        var clamp: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        var angle: Double
            get() = nativeUnitsToDegrees(pivotMotors.activeTrajectoryPosition)
            set(value) {
                setpoint = degreesToNativeUnits(value)
            }

        var intake: Double
            get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
            set(speed) {
                intakeMotorLeft.set(ControlMode.PercentOutput, speed)
                intakeMotorRight.set(ControlMode.PercentOutput, speed)
            }

        private const val ARM_TICKS_PER_DEGREE = 20.0 / 9.0
        private const val ARM_OFFSET_NATIVE = -640.0
        private fun nativeUnitsToDegrees(nativeUnits: Int): Double = (nativeUnits - ARM_OFFSET_NATIVE) / ARM_TICKS_PER_DEGREE
        private fun degreesToNativeUnits(degrees: Double): Double = degrees * ARM_TICKS_PER_DEGREE + ARM_OFFSET_NATIVE

        val position: Double
            get() = nativeUnitsToDegrees(pivotMotors.getSelectedSensorPosition(0))

        var setpoint: Double = position
            set(value) {
                field = value
                pivotMotors.set(ControlMode.Position, degreesToNativeUnits(value))
            }

        var clawClosed: Boolean
            get() = clawSolenoid.get()
            set(value) = clawSolenoid.set(value)

    }

}