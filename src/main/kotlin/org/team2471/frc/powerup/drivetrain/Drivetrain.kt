package org.team2471.frc.powerup.drivetrain

import com.analog.adis16448.frc.ADIS16448_IMU
import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.DemandType
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.windRelativeAngles
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.lib.vector.Vector2
import org.team2471.frc.powerup.*
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.CarriageConstants
import org.team2471.frc.powerup.carriage.Lifter
import org.team2471.frc.powerup.carriage.Pose
import java.lang.Math.*

object Drivetrain {
    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.1, 10)
        configOpenloopRamp(0.0, 10)
        inverted = true
        configOpenloopRamp(0.25, 10)
        configMotionProfileTrajectoryPeriod(0, 10)

//        configNominalOutputForward(DrivetrainConstants.MINIMUM_OUTPUT, 10)
//        configNominalOutputReverse(-DrivetrainConstants.MINIMUM_OUTPUT, 10)
//        configAllowableClosedloopError(0, feetToTicks(0.2).roundToInt(), 10)
        configNominalOutputForward(0.0, 10)
        configNominalOutputReverse(0.0, 10)
        configAllowableClosedloopError(0, 0, 10)

        // distance
        config_kP(0, DrivetrainConstants.DISTANCE_P, 10)
        config_kD(0, DrivetrainConstants.DISTANCE_D, 10)
        config_kF(0, 0.0, 10)

        selectProfileSlot(0, 0)
        Telemetry.registerMotor("Drive Left Master",this)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
        Telemetry.registerMotor("Drive Left Slave 1", this)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
        Telemetry.registerMotor("Drive Left Slave 2", this)
    }

    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.1, 10)
        configOpenloopRamp(0.0, 10)
        configMotionProfileTrajectoryPeriod(0, 10)

//        configNominalOutputForward(DrivetrainConstants.MINIMUM_OUTPUT, 10)
//        configNominalOutputReverse(-DrivetrainConstants.MINIMUM_OUTPUT, 10)
//        configAllowableClosedloopError(0, feetToTicks(0.2).roundToInt(), 10)
        configNominalOutputForward(0.0, 10)
        configNominalOutputReverse(0.0, 10)
        configAllowableClosedloopError(0, 0, 10)

        // distance
        config_kP(0, DrivetrainConstants.DISTANCE_P, 10)
        config_kD(0, DrivetrainConstants.DISTANCE_D, 10)
        config_kF(0, 0.0, 10)

        selectProfileSlot(0, 0)
        Telemetry.registerMotor("Drive Right Master", this)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        Telemetry.registerMotor("Drive Right Slave 1", this)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        Telemetry.registerMotor("Drive Right Slave 2", this)
    }

    private val gyro = ADIS16448_IMU(ADIS16448_IMU.Axis.kZ)

    val table = NetworkTableInstance.getDefault().getTable("Drivetrain")

    private fun ticksToFeet(ticks: Int) = ticks.toDouble() / DrivetrainConstants.TICKS_PER_REV * DrivetrainConstants.WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    private fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / DrivetrainConstants.WHEEL_DIAMETER_INCHES * DrivetrainConstants.TICKS_PER_REV

    private val gyroAngle: Double
        get() = gyro.angleZ + gyroAngleOffset

    var gyroAngleOffset = 0.0

    private val leftVelocity: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorVelocity(1) * 10)

    private val rightVelocity: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorVelocity(1) * 10)

    private val heightMultiplierCurve = MotionCurve().apply {
        storeValue(0.0, 1.0)
        storeValue(CarriageConstants.LIFTER_MAX_HEIGHT, 0.2)
    }

    private val rampRateCurve = MotionCurve().apply {
        storeValue(0.0, 0.1)
        storeValue(Pose.CARRY.lifterHeight, 0.1)
        storeValue(CarriageConstants.LIFTER_MAX_HEIGHT, 1.0)
    }

    private val leftDistance: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorPosition(0))

    private val rightDistance: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorPosition(0))

    private val throttleEntry = table.getEntry("Applied throttles")

    fun zeroGyro() = gyro.reset()

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val heightMultiplier = if(Carriage.targetPose == Pose.CLIMB_ACQUIRE_RUNG) 0.8 else heightMultiplierCurve.getValue(Lifter.height)
        leftPower *= heightMultiplier
        rightPower *= heightMultiplier

        leftPower = leftPower * (1.0 - DrivetrainConstants.MINIMUM_OUTPUT) + copySign(DrivetrainConstants.MINIMUM_OUTPUT, leftPower)
        rightPower = rightPower * (1.0 - DrivetrainConstants.MINIMUM_OUTPUT) + copySign(DrivetrainConstants.MINIMUM_OUTPUT, rightPower)


        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)

        var rampRate = rampRateCurve.getValue(Lifter.height)

        if (Carriage.targetPose == Pose.CLIMB) {
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

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    suspend fun driveAlongPath(path: Path2D, extraTime: Double = 0.0) {
        println("Driving along path ${path.name}, duration: ${path.durationWithSpeed}, travel direction: ${path.robotDirection}, mirrored: ${path.isMirrored}")
        path.resetDistances()

        zeroEncoders()
        var prevLeftDistance = 0.0
        var prevRightDistance = 0.0
        var prevLeftVelocity = 0.0
        var prevRightVelocity = 0.0
        var prevTime = 0.0

        val gyroAngleEntry = table.getEntry("Gyro Angle")
        val pathAngleEntry = table.getEntry("Path Angle")
        val angleErrorEntry = table.getEntry("Angle Error")
        val gyroCorrectionEntry = table.getEntry("Gyro Correction")

        val leftPositionError = table.getEntry("Left Position Error")
        val rightPositionError = table.getEntry("Right Position Error")

        val leftVelocityErrorEntry = table.getEntry("Left Velocity Error")
        val rightVelocityErrorEntry = table.getEntry("Right Velocity Error")

        val timer = Timer().apply { start() }

        var angleErrorAccum = 0.0
        var finished = false
        try {
            periodic(condition = { !finished }) {
                val t = timer.get()
                val dt = t - prevTime

                // apply gyro corrections to the distances
                val gyroAngle = gyroAngle
                val pathAngle = toDegrees(Vector2.angle(path.getTangent(t)))
                val angleError = pathAngle - windRelativeAngles(pathAngle, gyroAngle)

                angleErrorAccum = angleErrorAccum * DrivetrainConstants.GYRO_CORRECTION_I_DECAY + angleError

                val gyroCorrection = if (SmartDashboard.getBoolean("Use Gyro", true)) {
                    angleError * DrivetrainConstants.GYRO_CORRECTION_P + angleErrorAccum * DrivetrainConstants.GYRO_CORRECTION_I
                } else {
                    0.0
                }

                val leftDistance = path.getLeftDistance(t) + gyroCorrection
                val rightDistance = path.getRightDistance(t) - gyroCorrection

                val leftVelocity = (leftDistance - prevLeftDistance) / dt
                val rightVelocity = (rightDistance - prevRightDistance) / dt

                val leftVelocityError = this.leftVelocity - leftVelocity
                val rightVelocityError = this.rightVelocity - rightVelocity

                val velocityDeltaTimesCoefficient = (leftVelocity - rightVelocity) * DrivetrainConstants.TURNING_FEED_FORWARD

                gyroAngleEntry.setDouble(gyroAngle)
                pathAngleEntry.setDouble(pathAngle)
                angleErrorEntry.setDouble(angleError)
                leftPositionError.setDouble(ticksToFeet(leftMotors.getClosedLoopError(0)))
                rightPositionError.setDouble(ticksToFeet(rightMotors.getClosedLoopError(0)))
                leftVelocityErrorEntry.setDouble(leftVelocityError)
                rightVelocityErrorEntry.setDouble(rightVelocityError)

                gyroCorrectionEntry.setDouble(gyroCorrection)

                val leftFeedForward = leftVelocity * DrivetrainConstants.LEFT_FEED_FORWARD_COEFFICIENT +
                        (DrivetrainConstants.LEFT_FEED_FORWARD_OFFSET * signum(leftVelocity)) +
                        velocityDeltaTimesCoefficient

                val rightFeedForward = rightVelocity * DrivetrainConstants.RIGHT_FEED_FORWARD_COEFFICIENT +
                        (DrivetrainConstants.RIGHT_FEED_FORWARD_OFFSET * signum(rightVelocity)) -
                        velocityDeltaTimesCoefficient
                leftMotors.set(ControlMode.Position, feetToTicks(leftDistance),
                        DemandType.ArbitraryFeedForward, leftFeedForward)
                rightMotors.set(ControlMode.Position, feetToTicks(rightDistance),
                        DemandType.ArbitraryFeedForward, rightFeedForward)
//                leftMotors.set(ControlMode.PercentOutput, 0.0, DemandType.ArbitraryFeedForward, leftFeedForward)
//                rightMotors.set(ControlMode.PercentOutput, 0.0, DemandType.ArbitraryFeedForward, rightFeedForward)

                val leftDistanceError = ticksToFeet(leftMotors.getClosedLoopError(0))
                val rightDistanceError = ticksToFeet(rightMotors.getClosedLoopError(0))
                // give up if the error is too large
//                if (abs(leftDistanceError) > DrivetrainConstants.MAX_PATH_ERROR ||
//                        abs(rightDistanceError) > DrivetrainConstants.MAX_PATH_ERROR) {
//                    throw CancellationException("Path following error too high")
//                }


                if (leftMotors.motorOutputPercent > 0.95) {
                    DriverStation.reportWarning("Left motor is saturated", false)
                }
                if (rightMotors.motorOutputPercent > 0.95) {
                    DriverStation.reportWarning("Right motor is saturated", false)
                }

                finished = t >= path.durationWithSpeed + extraTime
//                        && abs(angleError) < 3.5

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

    fun zeroEncoders() {
        leftMotors.sensorCollection.setQuadraturePosition(0, 0)
        rightMotors.sensorCollection.setQuadraturePosition(0, 0)
    }

    fun calibrateGyro() {
        val prevLEDState = LEDController.state
        LEDController.state = CallibrateGyroState
        println("Gyro calibrated in ${measureTimeFPGA {
            gyro.calibrate()
        }} seconds")
        LEDController.state = prevLEDState
    }

    init {
        val compassEntry = table.getEntry("Compass Angle")

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

        SmartDashboard.putBoolean("Use Gyro", true)

        launch {
            val leftVelocityEntry = table.getEntry("Left Velocity")
            val rightVelocityEntry = table.getEntry("Right Velocity")
            SmartDashboard.putData("Gyro", gyro)
            val accelXEntry = table.getEntry("Accelerometer X")
            val accelYEntry = table.getEntry("Accelerometer Y")
            val accelZEntry = table.getEntry("Accelerometer Z")

            periodic {
                compassEntry.setDouble(gyro.magZ)
                leftVelocityEntry.setDouble(leftVelocity)
                rightVelocityEntry.setDouble(rightVelocity)

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


val driveTest = Command("Drive Test", Drivetrain) {
    val throttle = SmartDashboard.getNumber("Test Throttle", 0.0)
    Drivetrain.driveRaw(throttle, throttle)
    try {
        delay(3000)
    } finally {
        Drivetrain.driveRaw(0.0, 0.0)
    }

}