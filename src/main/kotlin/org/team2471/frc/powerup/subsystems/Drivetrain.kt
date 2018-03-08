package org.team2471.frc.powerup.subsystems

import com.analog.adis16448.frc.ADIS16448_IMU
import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.RobotMap
import java.lang.Math.copySign

object Drivetrain {
    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(20, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.0, 10)
        configOpenloopRamp(0.0, 10)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.0, 10)
        inverted = true
        configOpenloopRamp(0.25, 10)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(20, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        inverted = true
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(20, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        inverted = true
    }

    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(20, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.0, 10)
        configOpenloopRamp(0.0, 10)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.5, 10)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(20, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configContinuousCurrentLimit(5, 10)
        configPeakCurrentLimit(0, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
    }

    private val gyro = ADIS16448_IMU()

    val table = NetworkTableInstance.getDefault().getTable("Drivetrain")

    private const val TICKS_PER_REV = 783
    private const val WHEEL_DIAMETER_INCHES = 5.0
    private const val MINIMUM_OUTPUT = 0.04

    fun ticksToFeet(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / WHEEL_DIAMETER_INCHES * TICKS_PER_REV

    val gyroAngle: Double
        get() = gyro.angleZ


    private val heightMultiplierCurve = MotionCurve().apply {
        storeValue(0.0, 1.0)
        storeValue(Carriage.Lifter.MAX_HEIGHT, 0.4)
    }

    private val rampRateCurve = MotionCurve().apply {
        storeValue(0.0, 0.1)
        storeValue(Carriage.Pose.CARRY.lifterHeight, 0.1)
        storeValue(Carriage.Lifter.MAX_HEIGHT, 2.5)
    }

    private val leftPosition: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorPosition(0))

    private val rightPosition: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorPosition(0))

    private val throttleEntry = table.getEntry("Applied throttles")

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {

        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val heightMultiplier = heightMultiplierCurve.getValue(Carriage.Lifter.height)
        leftPower *= heightMultiplier
        rightPower *= heightMultiplier

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        leftPower = leftPower * (1.0 - MINIMUM_OUTPUT) + copySign(MINIMUM_OUTPUT, leftPower)
        rightPower = rightPower * (1.0 - MINIMUM_OUTPUT) + copySign(MINIMUM_OUTPUT, rightPower)

        var rampRate = rampRateCurve.getValue(Carriage.Lifter.height)

        if (Carriage.targetPose == Carriage.Pose.CLIMB) {
            rampRate *= 0.6
        }

        leftMotors.configOpenloopRamp(rampRate, 0)
        rightMotors.configOpenloopRamp(rampRate, 0)

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)

        throttleEntry.setDoubleArray(doubleArrayOf(leftPower, rightPower))
    }

    suspend fun driveDistance(distance: Double, time: Double, suspend: Boolean = true) {
        val curve = MotionCurve()
        curve.storeValue(0.0, 0.0)
        curve.storeValue(time, distance)
        try {
            zeroDistance()

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= time }) {
                val t = timer.get()
                val v = curve.getValue(t)
                leftMotors.set(ControlMode.Position, feetToTicks(v))
                rightMotors.set(ControlMode.Position, feetToTicks(v))
            }

            if (suspend)
                suspendUntil { Math.abs(leftPosition - distance) < 0.1 && Math.abs(rightPosition - distance) < 0.1 }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }

    suspend fun turnInPlace(angle: Double, time: Double) {
        val curve = MotionCurve()
        val robotWidth = 25.0 / 12.0
        val scrubFactor = 1.5
        val dist = ((Math.PI * robotWidth) / 360) * angle * scrubFactor
        curve.storeValue(0.0, 0.0)
        curve.storeValue(time, dist)
        zeroDistance()
        val timer = Timer().apply { start() }
        try {
            periodic(condition = { timer.get() <= time }) {
                val t = timer.get()
                val v = curve.getValue(t)
                leftMotors.set(ControlMode.Position, feetToTicks(-v))
                rightMotors.set(ControlMode.Position, feetToTicks(v))
            }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }

    fun setDistance(distance: Double) {
        leftMotors.set(ControlMode.Position, feetToTicks(distance))
        rightMotors.set(ControlMode.Position, feetToTicks(distance))
    }

    fun zeroDistance() {
        leftMotors.sensorCollection.setQuadraturePosition(0, 0)
        rightMotors.sensorCollection.setQuadraturePosition(0, 0)
    }

    suspend fun driveAlongPath(path2D: Path2D) {
        println("Driving along path ${path2D.name}, duration: ${path2D.durationWithSpeed}, travel direction: ${path2D.robotDirection}, mirrored: ${path2D.isMirrored}")
        path2D.resetDistances()
        try {
            leftMotors.sensorCollection.setQuadraturePosition(0, 0)
            rightMotors.sensorCollection.setQuadraturePosition(0, 0)

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= path2D.durationWithSpeed }) {
                val t = timer.get()
                val leftDistance = path2D.getLeftDistance(t)
                val rightDistance = path2D.getRightDistance(t)
                SmartDashboard.putNumberArray("Distance", arrayOf(leftDistance, rightDistance))
                val leftError = leftDistance - leftPosition
                val rightError = rightDistance - rightPosition
                SmartDashboard.putNumberArray("Errors", arrayOf(leftError, rightError))
                SmartDashboard.putNumber("Delta", leftDistance - rightDistance)
                SmartDashboard.putNumber("Delta Error", leftError - rightError)
                leftMotors.set(ControlMode.Position, feetToTicks(leftDistance))
                rightMotors.set(ControlMode.Position, feetToTicks(rightDistance))
            }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }

    init {
        gyro.calibrate()

        val pEntry = table.getEntry("Position P")
        val dEntry = table.getEntry("Position D")
        pEntry.setDouble(Double.NaN)
        dEntry.setDouble(Double.NaN)

        pEntry.addListener({ event ->
            leftMotors.config_kP(0, event.value.double, 0)
            rightMotors.config_kP(0, event.value.double, 0)
        }, EntryListenerFlags.kUpdate)
        dEntry.addListener({ event ->
            leftMotors.config_kD(0, event.value.double, 0)
            rightMotors.config_kD(0, event.value.double, 0)
        }, EntryListenerFlags.kUpdate)

        launch {
            val velocityEntry = table.getEntry("Velocity")
            val outputEntry = table.getEntry("Output")

            periodic {
                velocityEntry.setDoubleArray(doubleArrayOf(
                        ticksToFeet(leftMotors.getSelectedSensorVelocity(0)) * 10.0,
                        ticksToFeet(rightMotors.getSelectedSensorVelocity(0)) * 10.0))

                outputEntry.setDoubleArray(doubleArrayOf(
                        leftMotors.motorOutputPercent,
                        rightMotors.motorOutputPercent
                ))
                SmartDashboard.putNumberArray("Encoder Distances",
                        arrayOf(ticksToFeet(leftMotors.getSelectedSensorPosition(0)),
                                ticksToFeet(rightMotors.getSelectedSensorPosition(0))))
            }
        }

        CommandSystem.registerDefaultCommand(this, Command("Drivetrain Default", this) {
            periodic {
                SmartDashboard.putNumber("Gyro Angle", gyro.angleZ)
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }
}