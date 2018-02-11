package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.Solenoid
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.math.clamp
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.RobotMap

object Carriage {
    private const val CARRIAGE_SRX_UNITS_TO_INCHES = (1.75 * Math.PI) / 750 // TODO: check for later

    private const val TICKS_PER_INCH = 7550.0 / 64.25

    private const val FEED_FORWARD_UP = 1.705

    private val liftMotors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setSelectedSensorPosition(0, 0, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        configPeakOutputForward(1.0, 10)
        configPeakOutputReverse(-1.0, 10)
        config_kP(0, 0.0, 10)
        config_kI(0, 0.0, 10)
        config_kD(0, 0.0, 10)
        config_kF(0, FEED_FORWARD_UP, 10)
        configMotionCruiseVelocity(850, 10)
        configMotionAcceleration(300, 10)

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

    var height: Double
        get() = ticksToInches(liftMotors.getSelectedSensorPosition(0))
        set(value) = liftMotors.set(ControlMode.MotionMagic, ticksToInches(value.toInt()))

    var isLowGear: Boolean
        get() = shifter.get()
        set(value) = shifter.set(value)

    var isBraking: Boolean
        get() = !discBrake.get()
        set(value) = discBrake.set(!value)

    init {
        CommandSystem.registerDefaultCommand(this, Command("Carriage Default", this) {
            periodic {
                Arm.angle = CoDriver.wristPivot * 90.0 + 90.0

                val liftSpeed = CoDriver.updown
                liftMotors.set(ControlMode.PercentOutput, liftSpeed)
                isBraking = liftSpeed == 0.0
                isLowGear = CoDriver.shift
            }
        })

        launch {
            val table = NetworkTableInstance.getDefault().getTable("Carriage")
            val sensorPositionEntry = table.getEntry("Sensor Position")
            val sensorVelocityEntry = table.getEntry("Sensor Velocity")
            val activeTrajectoryPositionEntry = table.getEntry("Active Trajectory Position")
            val activeTrajectoryVelocityEntry = table.getEntry("Active Trajectory Velocity")
            val appliedMotorOutputEntry = table.getEntry("Applied Motor Output")
            val closedLoopErrEntry = table.getEntry("Closed Loop Error")
            val minVelocityEntry = table.getEntry("Minimum Velocity")
            val maxVelocityEntry = table.getEntry("Maximum Velocity")

            var prevVelocity = 0.0
            periodic {
                sensorPositionEntry.setDouble(liftMotors.getSelectedSensorPosition(0).toDouble())
                val velocity = liftMotors.getSelectedSensorVelocity(0).toDouble()
                prevVelocity = velocity
                sensorVelocityEntry.setDouble(velocity)
                activeTrajectoryPositionEntry.setDouble(liftMotors.activeTrajectoryPosition.toDouble())
                activeTrajectoryVelocityEntry.setDouble(liftMotors.activeTrajectoryVelocity.toDouble())
                appliedMotorOutputEntry.setDouble(liftMotors.motorOutputPercent)
                closedLoopErrEntry.setDouble(liftMotors.getClosedLoopError(0).toDouble())
                minVelocityEntry.setDouble(Math.min(velocity, minVelocityEntry.getDouble(0.0)))
                maxVelocityEntry.setDouble(Math.max(velocity, maxVelocityEntry.getDouble(0.0)))
            }

        }
    }

    private const val TICKS_PER_REV = 783
    private const val SPOOL_DIAMETER_INCHES = 2.0
    private fun ticksToInches(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * SPOOL_DIAMETER_INCHES * Math.PI

    suspend fun moveToHeight(height: Double) {
        this.height = height
        try {
            isBraking = false
            isLowGear = false
            suspendUntil { Math.abs(this.height - height) < 2.5 }
        } finally {
            isBraking = true
            isLowGear = true
        }
    }

    suspend fun moveToPose(pose: Pose) {
        val safeRange = 0.0..110.0
        Arm.angle = safeRange.clamp(Arm.angle)
        suspendUntil { Arm.angle in safeRange }
        height = pose.inches
        suspendUntil { /*Far enough to move the arm to position */ false }
        Arm.angle = pose.armAngle
        suspendUntil { /* done */ false }
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
            setSensorPhase(false)
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

        val hasCube: Boolean
            get() = cubeSensor.voltage < 0.15

        var clamp: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        var angle: Double
            get() = nativeUnitsToDegrees(pivotMotors.getSelectedSensorPosition(0))
            set(value) = pivotMotors.set(ControlMode.Position, degreesToNativeUnits(value))

        var intake: Double
            get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
            set(speed) {
                intakeMotorLeft.set(ControlMode.PercentOutput, speed)
                intakeMotorRight.set(ControlMode.PercentOutput, speed)
            }

        private const val ARM_TICKS_PER_DEGREE = 20.0 / 9.0
        private const val ARM_OFFSET_NATIVE = -720.0
        private fun nativeUnitsToDegrees(nativeUnits: Int): Double = (nativeUnits - ARM_OFFSET_NATIVE) / ARM_TICKS_PER_DEGREE
        private fun degreesToNativeUnits(degrees: Double): Double = degrees * ARM_TICKS_PER_DEGREE + ARM_OFFSET_NATIVE

        var clawClosed: Boolean
            get() = clawSolenoid.get()
            set(value) = clawSolenoid.set(value)

    }

}