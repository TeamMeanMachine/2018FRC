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
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.math.deadband
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
    private const val TURNING_KP = 0.002
    private const val DRIVE_COMPENSATOR = 0.9

    val leftMaster = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10)
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.1, 10)
        configOpenloopRamp(0.0, 10)
        inverted = true

        config_kP(0, DrivetrainConstants.DISTANCE_P, 10)
        config_kD(0, DrivetrainConstants.DISTANCE_D, 10)
        config_kF(0, 0.0, 10)

        selectProfileSlot(0, 0)
        Telemetry.registerMotor("Drive Left Master",this)
    }
    private val leftSlave1 = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        set(ControlMode.Follower, leftMaster.deviceID.toDouble())
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
        Telemetry.registerMotor("Drive Left Slave 1", this)
    }
    private val leftSlave2 = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3).apply {
        set(ControlMode.Follower, leftMaster.deviceID.toDouble())
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        inverted = true
        Telemetry.registerMotor("Drive Left Slave 2", this)
    }

    val rightMaster = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10)
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        configClosedloopRamp(0.1, 10)
        configOpenloopRamp(0.0, 10)

        config_kP(0, DrivetrainConstants.DISTANCE_P, 10)
        config_kD(0, DrivetrainConstants.DISTANCE_D, 10)
        config_kF(0, 0.0, 10)

        selectProfileSlot(0, 0)
        Telemetry.registerMotor("Drive Right Master",this)
    }
    private val rightSlave1 = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Coast)
        set(ControlMode.Follower, rightMaster.deviceID.toDouble())
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        Telemetry.registerMotor("Drive Right Slave 1", this)
    }
    private val rightSlave2 = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
        set(ControlMode.Follower, rightMaster.deviceID.toDouble())
        configPeakCurrentLimit(DrivetrainConstants.PEAK_CURRENT_LIMIT, 10)
        configContinuousCurrentLimit(DrivetrainConstants.CONTINUOUS_CURRENT_LIMIT, 10)
        configPeakCurrentDuration(DrivetrainConstants.PEAK_CURRENT_DURATION, 10)
        enableCurrentLimit(true)
        Telemetry.registerMotor("Drive Right Slave 2", this)
    }

    //private val leftSpeedController = SpeedControllerGroup(leftMaster)
    //private val differentialDrive = DifferentialDrive(leftMaster, rightMaster)

    private val gyro = ADIS16448_IMU(ADIS16448_IMU.Axis.kZ)
//    private val gyro = edu.wpi.first.wpilibj.ADXRS450_Gyro()

    private val table = NetworkTableInstance.getDefault().getTable("Drivetrain")

    private fun ticksToFeet(ticks: Int) = ticks.toDouble() / DrivetrainConstants.TICKS_PER_REV * DrivetrainConstants.WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    private fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / DrivetrainConstants.WHEEL_DIAMETER_INCHES * DrivetrainConstants.TICKS_PER_REV

    var gyroAngleOffset = 0.0

    var isBraking = true
        set(value) {
            val neutralMode = if (value) NeutralMode.Brake else NeutralMode.Coast
            leftSlave2.setNeutralMode(neutralMode)
            rightSlave2.setNeutralMode(neutralMode)
            field = value
        }

    private val gyroAngle: Double
        get() = gyro.angleZ + gyroAngleOffset

    val leftVelocity: Double
        get() = ticksToFeet(leftMaster.getSelectedSensorVelocity(1) * 10)

    val rightVelocity: Double
        get() = ticksToFeet(rightMaster.getSelectedSensorVelocity(1) * 10)

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
        get() = ticksToFeet(leftMaster.getSelectedSensorPosition(0))

    private val rightDistance: Double
        get() = ticksToFeet(rightMaster.getSelectedSensorPosition(0))

    private val throttleEntry = table.getEntry("Applied throttles")

    fun zeroGyro() = gyro.reset()

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        val totalTurn = (softTurn * Math.abs(throttle)) + hardTurn
        val velocitySetpoint = totalTurn * 250.0
        val gyroRate = gyro.rateZ
        val velocityError = velocitySetpoint - gyroRate

        val turnAdjust = (velocityError * TURNING_KP).deadband( 1.0e-2)

        var leftPower = throttle + totalTurn + turnAdjust
        var rightPower = throttle - totalTurn - turnAdjust

        SmartDashboard.putNumber("Gyro Rate", gyroRate)

        val heightMultiplier = if(Carriage.targetPose == Pose.CLIMB_ACQUIRE_RUNG) 0.8 else heightMultiplierCurve.getValue(Lifter.height)
        leftPower *= heightMultiplier
        rightPower *= heightMultiplier

        leftPower = leftPower * (1.0 - DrivetrainConstants.MINIMUM_OUTPUT) + copySign(DrivetrainConstants.MINIMUM_OUTPUT, leftPower)
        rightPower = rightPower * DRIVE_COMPENSATOR *(1.0 - DrivetrainConstants.MINIMUM_OUTPUT) + copySign(DrivetrainConstants.MINIMUM_OUTPUT, rightPower)


        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        leftMaster.set(ControlMode.PercentOutput, leftPower)
        rightMaster.set(ControlMode.PercentOutput, rightPower)

        var rampRate = rampRateCurve.getValue(Lifter.height)

        if (Carriage.targetPose == Pose.CLIMB) {
            rampRate *= 0.6
        }

        leftMaster.configOpenloopRamp(rampRate, 0)
        rightMaster.configOpenloopRamp(rampRate, 0)


        throttleEntry.setDoubleArray(doubleArrayOf(leftPower, rightPower))
    }

    fun driveRaw(leftSpeed: Double, rightSpeed: Double) {
        leftMaster.set(ControlMode.PercentOutput, leftSpeed)
        rightMaster.set(ControlMode.PercentOutput, rightSpeed)
    }

    suspend fun driveDistance(distance: Double, time: Double, suspend: Boolean = true) {
        leftMaster.selectProfileSlot(0, 0)
        rightMaster.selectProfileSlot(0, 0)
        val curve = MotionCurve()
        curve.storeValue(0.0, 0.0)
        curve.storeValue(time, distance)
        try {
            zeroDistance()

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= time }) {
                val t = timer.get()
                val v = curve.getValue(t)
                leftMaster.set(ControlMode.Position, feetToTicks(v))
                rightMaster.set(ControlMode.Position, feetToTicks(v))
            }

            if (suspend)
                suspendUntil { Math.abs(leftDistance - distance) < 0.1 && Math.abs(rightDistance - distance) < 0.1 }
        } finally {
            leftMaster.neutralOutput()
            rightMaster.neutralOutput()
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
                leftMaster.set(ControlMode.Position, feetToTicks(-v))
                rightMaster.set(ControlMode.Position, feetToTicks(v))
            }
        } finally {
            leftMaster.neutralOutput()
            rightMaster.neutralOutput()
        }
    }

    fun zeroDistance() {
        leftMaster.sensorCollection.setQuadraturePosition(0, 0)
        rightMaster.sensorCollection.setQuadraturePosition(0, 0)
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

        val pathAngleEntry = table.getEntry("Path Angle")
        val angleErrorEntry = table.getEntry("Angle Error")
        val gyroCorrectionEntry = table.getEntry("Gyro Correction")

        val leftPositionErrorEntry = table.getEntry("Left Position Error")
        val rightPositionErrorEntry = table.getEntry("Right Position Error")

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

                pathAngleEntry.setDouble(pathAngle)
                angleErrorEntry.setDouble(angleError)
                leftPositionErrorEntry.setDouble(ticksToFeet(leftMaster.getClosedLoopError(0)))
                rightPositionErrorEntry.setDouble(ticksToFeet(rightMaster.getClosedLoopError(0)))
                leftVelocityErrorEntry.setDouble(leftVelocityError)
                rightVelocityErrorEntry.setDouble(rightVelocityError)

                gyroCorrectionEntry.setDouble(gyroCorrection)

                val leftFeedForward = leftVelocity * DrivetrainConstants.LEFT_FEED_FORWARD_COEFFICIENT +
                        (DrivetrainConstants.LEFT_FEED_FORWARD_OFFSET * signum(leftVelocity)) +
                        velocityDeltaTimesCoefficient

                val rightFeedForward = rightVelocity * DrivetrainConstants.RIGHT_FEED_FORWARD_COEFFICIENT +
                        (DrivetrainConstants.RIGHT_FEED_FORWARD_OFFSET * signum(rightVelocity)) -
                        velocityDeltaTimesCoefficient

                leftMaster.set(ControlMode.Position, feetToTicks(leftDistance),
                        DemandType.ArbitraryFeedForward, leftFeedForward)
                rightMaster.set(ControlMode.Position, feetToTicks(rightDistance),
                        DemandType.ArbitraryFeedForward, rightFeedForward)

                if (leftMaster.motorOutputPercent > 0.95) {
                    DriverStation.reportWarning("Left motor is saturated", false)
                }
                if (rightMaster.motorOutputPercent > 0.95) {
                    DriverStation.reportWarning("Right motor is saturated", false)
                }

                finished = t >= path.durationWithSpeed + extraTime

                prevTime = t
                prevLeftDistance = leftDistance
                prevRightDistance = rightDistance
                prevLeftVelocity = leftVelocity
                prevRightVelocity = rightVelocity
            }
        } finally {
            leftMaster.neutralOutput()
            rightMaster.neutralOutput()
        }
    }

    fun zeroEncoders() {
        leftMaster.sensorCollection.setQuadraturePosition(0, 0)
        rightMaster.sensorCollection.setQuadraturePosition(0, 0)
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
            leftMaster.config_kP(0, event.value.double, 0)
            rightMaster.config_kP(0, event.value.double, 0)
        }, EntryListenerFlags.kUpdate)
        dEntry.addListener({ event ->
            leftMaster.config_kD(0, event.value.double, 0)
            rightMaster.config_kD(0, event.value.double, 0)
        }, EntryListenerFlags.kUpdate)

        SmartDashboard.putBoolean("Use Gyro", true)

        launch {
            val leftPositionEntry = table.getEntry("Left Position")
            val rightPositionEntry = table.getEntry("Right Position")

            val leftVelocityEntry = table.getEntry("Left Velocity")
            val rightVelocityEntry = table.getEntry("Right Velocity")
            val gyroAngleEntry = table.getEntry("Gyro Angle")
            SmartDashboard.putData("Gyro", gyro)

            val outputsEntry = table.getEntry("Outputs")

            periodic {
                gyroAngleEntry.setDouble(gyroAngle)
                leftVelocityEntry.setDouble(leftVelocity)
                rightVelocityEntry.setDouble(rightVelocity)

                leftPositionEntry.setDouble(leftDistance)
                rightPositionEntry.setDouble(rightDistance)

                outputsEntry.setDoubleArray(doubleArrayOf(leftMaster.motorOutputPercent, rightMaster.motorOutputPercent))

                SmartDashboard.putNumberArray("Encoder Distances",
                        arrayOf(ticksToFeet(leftMaster.getSelectedSensorPosition(0)),
                                ticksToFeet(rightMaster.getSelectedSensorPosition(0))))
            }
        }

        CommandSystem.registerDefaultCommand(this, Command("Drivetrain Default", this) {
            periodic {
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)

//                println("Left Output: ${leftMaster.motorOutputPercent}")
//                println("Left Current: ${leftMaster.outputCurrent}")
//                println("Right Output: ${rightMaster.motorOutputPercent}")
//                println("Right Current: ${rightMaster.outputCurrent}")
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