package org.team2471.frc.powerup.drivetrain

import com.analog.adis16448.frc.ADIS16448_IMU
import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.RobotMap
import org.team2471.frc.powerup.carriage.Carriage
import java.lang.Math.abs
import java.lang.Math.copySign

object Drivetrain {
    private const val PEAK_CURRENT_LIMIT = 20
    private const val CONTINUOUS_CURRENT_LIMIT = 15
    private const val PEAK_CURRENT_DURATION = 0

    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.0, 10)
        configOpenloopRamp(0.0, 10)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.0, 10)
        inverted = true
        configOpenloopRamp(0.25, 10)
        configMotionProfileTrajectoryPeriod(5, 10)

        // distance
//        config_kP(0, 2.0, 10)
//        config_kD(0, 0.5, 10)

        // velocity
        config_kP(0, 3.0, 10)
        config_kD(0, 0.0, 10)
        config_kF(0, LEFT_FEED_FORWARD_COEFFICIENT * 1023.0 / 10.0 / 6.25, 10)

        selectProfileSlot(0, 0)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
    }

    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.0, 10)
        configOpenloopRamp(0.0, 10)
        configMotionProfileTrajectoryPeriod(5, 10)

        // distance
//        config_kP(0, 2.0, 10)
//        config_kD(0, 0.5, 10)

        // velocity
        config_kP(0, 3.0, 10)
        config_kD(0, 0.0, 10)
        config_kF(0, RIGHT_FEED_FORWARD_COEFFICIENT * 1023.0 / 10.0 / 6.25, 10)

        selectProfileSlot(0, 0)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
    }

    fun loadTrajectory(trajectory: Trajectory) = launch {
        leftMotors.clearMotionProfileTrajectories()
        rightMotors.clearMotionProfileTrajectories()
        trajectory.forEach { (leftTrajectoryPoint, rightTrajectoryPoint) ->
            leftMotors.pushMotionProfileTrajectory(leftTrajectoryPoint)
            rightMotors.pushMotionProfileTrajectory(rightTrajectoryPoint)
            yield()
        }
    }

    suspend fun runSavedTrajectory() {
//        periodic()
    }

    private val gyro = ADIS16448_IMU()

    val table = NetworkTableInstance.getDefault().getTable("Drivetrain")

    private const val TICKS_PER_REV = 795
    private const val WHEEL_DIAMETER_INCHES = 5.0
    private const val MINIMUM_OUTPUT = 0.04

    fun ticksToFeet(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / WHEEL_DIAMETER_INCHES * TICKS_PER_REV

    val gyroAngle: Double
        get() = gyro.angleZ

    private var leftVelocity: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorVelocity(1) * 10)
        set(value) = leftMotors.set(ControlMode.Velocity, feetToTicks(value / 10.0).also {
            table.getEntry("Left Velocity Setpoint").setDouble(it)
        })


    private var rightVelocity: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorVelocity(1) * 10)
        set(value) = rightMotors.set(ControlMode.Velocity, feetToTicks(value / 10.0).also {
            table.getEntry("Right Velocity Setpoint").setDouble(it)
        })

    private val heightMultiplierCurve = MotionCurve().apply {
        storeValue(0.0, 1.0)
        storeValue(Carriage.Lifter.MAX_HEIGHT, 0.4)
    }

    private val rampRateCurve = MotionCurve().apply {
        storeValue(0.0, 0.1)
        storeValue(Carriage.Pose.CARRY.lifterHeight, 0.1)
        storeValue(Carriage.Lifter.MAX_HEIGHT, 2.5)
    }

    private val leftDistance: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorPosition(0))

    private val rightDistance: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorPosition(0))

    private val throttleEntry = table.getEntry("Applied throttles")

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val heightMultiplier = heightMultiplierCurve.getValue(Carriage.Lifter.height)
        leftPower *= heightMultiplier
        rightPower *= heightMultiplier

//        leftPower = (leftPower * MAX_SPEED * LEFT_FEED_FORWARD_COEFFICIENT) + (LEFT_FEED_FORWARD_OFFSET * signum(leftPower))
//        rightPower = (rightPower * MAX_SPEED * RIGHT_FEED_FORWARD_COEFFICIENT) + (RIGHT_FEED_FORWARD_OFFSET * signum(rightPower))
        leftPower = leftPower * (1.0 - MINIMUM_OUTPUT) + copySign(MINIMUM_OUTPUT, leftPower)
        rightPower = rightPower * (1.0 - MINIMUM_OUTPUT) + copySign(MINIMUM_OUTPUT, rightPower)


        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)

        var rampRate = rampRateCurve.getValue(Carriage.Lifter.height)

        if (Carriage.targetPose == Carriage.Pose.CLIMB) {
            rampRate *= 0.6
        }

        leftMotors.configOpenloopRamp(rampRate, 0)
        rightMotors.configOpenloopRamp(rampRate, 0)


        throttleEntry.setDoubleArray(doubleArrayOf(leftPower, rightPower))
    }

    fun driveRaw(leftSpeed: Double, rightSpeed: Double) {
        leftMotors.set(ControlMode.PercentOutput, leftSpeed)
        rightMotors.set(ControlMode.PercentOutput, rightSpeed)
    }

    private const val MAX_SPEED = 12.82
    private const val ACCELERATION_COEFFICIENT = 0.1454439814814
    private const val DISTANCE_COEFFICIENT = 5.0

    private const val LEFT_FEED_FORWARD_COEFFICIENT = 0.078479461930836 * 1.1
    private const val LEFT_FEED_FORWARD_OFFSET = 0.010218260839712

    private const val RIGHT_FEED_FORWARD_COEFFICIENT = 0.078175224207892
    private const val RIGHT_FEED_FORWARD_OFFSET = 0.00081904993955539

    fun driveVelocity(leftVelocity: Double, rightVelocity: Double) {
        if (abs(leftVelocity) > MAX_SPEED) DriverStation.reportWarning("Left Velocity target $leftVelocity exceeds max speed $MAX_SPEED", false)
        if (abs(rightVelocity) > MAX_SPEED) DriverStation.reportWarning("Right Velocity target $rightVelocity exceeds max speed $MAX_SPEED", false)

        Drivetrain.leftVelocity = leftVelocity
        Drivetrain.rightVelocity = rightVelocity
//        leftMotors.set(ControlMode.PercentOutput, leftVelocity * LEFT_FEED_FORWARD_COEFFICIENT +
//                LEFT_FEED_FORWARD_OFFSET * signum(leftVelocity))
//
//        rightMotors.set(ControlMode.PercentOutput, rightVelocity * RIGHT_FEED_FORWARD_COEFFICIENT +
//                RIGHT_FEED_FORWARD_OFFSET * signum(rightVelocity))

    }

    suspend fun driveDistance(distance: Double, time: Double, suspend: Boolean = true) {
        leftMotors.selectProfileSlot(0, 0)
        rightMotors.selectProfileSlot(0, 0)
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
                suspendUntil { Math.abs(leftDistance - distance) < 0.1 && Math.abs(rightDistance - distance) < 0.1 }
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

    suspend fun driveAlongPath(path: Path2D) {
        println("Driving along path ${path.name}, duration: ${path.durationWithSpeed}, travel direction: ${path.robotDirection}, mirrored: ${path.isMirrored}")
        path.resetDistances()

        zero()
        var prevLeftDistance = 0.0
        var prevRightDistance = 0.0
        var prevLeftVelocity = 0.0
        var prevRightVelocity = 0.0
        var prevTime = 0.0

        val timer = Timer().apply { start() }
        try {
            periodic(condition = { timer.get() <= path.durationWithSpeed }) {
                val t = timer.get()
                val leftDistance = path.getLeftDistance(t)
                val rightDistance = path.getRightDistance(t)
                println("t: $t, Left: $leftDistance, Right: $rightDistance")

                val leftDistanceError = leftDistance - this.leftDistance
                val rightDistanceError = rightDistance - this.rightDistance

                val dt = t - prevTime
                val leftVelocity = (leftDistance - prevLeftDistance) / dt
                val rightVelocity = (rightDistance - prevRightDistance) / dt
                val leftAcceleration = (leftVelocity - prevLeftVelocity) / dt
                val rightAcceleration = (rightVelocity - prevRightVelocity) / dt

                SmartDashboard.putNumber("Left Acceleration", leftAcceleration)
                SmartDashboard.putNumber("Right Acceleration", rightAcceleration)

                val leftOutputVelocity = leftVelocity +
                        leftAcceleration * ACCELERATION_COEFFICIENT +
                        leftDistanceError * DISTANCE_COEFFICIENT

                val rightOutputVelocity = rightVelocity +
                        rightAcceleration * ACCELERATION_COEFFICIENT +
                        rightDistanceError * DISTANCE_COEFFICIENT
                driveVelocity(leftOutputVelocity, rightOutputVelocity)

                val leftError = this.leftVelocity - leftVelocity
                val rightError = this.rightVelocity - rightVelocity

                table.getEntry("Target Velocity").setDoubleArray(doubleArrayOf(leftVelocity, rightVelocity))
                table.getEntry("Velocity Errors").setDoubleArray(doubleArrayOf(leftError, rightError))
                table.getEntry("Left Distance Error").setDouble(leftDistanceError)
                table.getEntry("Right Distance Error").setDouble(rightDistanceError)
                table.getEntry("Left Velocity Error").setDouble(leftError)
                table.getEntry("Right Velocity Error").setDouble(rightError)

                prevTime = t
                prevLeftDistance = leftDistance
                prevRightDistance = rightDistance
                prevLeftVelocity = leftVelocity
                prevRightVelocity = rightVelocity
            }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }

    fun zero() {
        leftMotors.sensorCollection.setQuadraturePosition(0, 0)
        rightMotors.sensorCollection.setQuadraturePosition(0, 0)
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
            val outputEntry = table.getEntry("Output")
            val leftVelocityEntry = table.getEntry("Left Velocity")
            val rightVelocityEntry = table.getEntry("Right Velocity")

            periodic {
                leftVelocityEntry.setDouble(leftVelocity)
                rightVelocityEntry.setDouble(rightVelocity)

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
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }
}

val driveVelocity = Command("Drive Velocity", Drivetrain) {
    periodic {
        val throttle = Driver.throttle
        val turn = Driver.softTurn

        val leftVelocity = (throttle + turn) * 12.0
        val rightVelocity = (throttle - turn) * 12.0
        Drivetrain.driveVelocity(leftVelocity, rightVelocity)
    }
}

val driveTest = Command("Drive Test", Drivetrain) {
    val throttle = SmartDashboard.getNumber("Test Throttle", 0.0)
    Drivetrain.driveRaw(throttle, throttle)
    try {
        delay(3000)
    } finally {
        Drivetrain.driveRaw(0.0, 0.0)
    }

}