package org.team2471.frc.powerup.subsystems

import com.analog.adis16448.frc.ADIS16448_IMU
import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
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

object Drivetrain {
    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.0, 10)
        inverted = true
        configOpenloopRamp(0.25, 10)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
        inverted = true
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
        inverted = true
    }

    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setNeutralMode(NeutralMode.Brake)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
        configOpenloopRamp(0.25, 10)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.5, 10)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Coast)
        configPeakCurrentLimit(25, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentDuration(500, 10)
        enableCurrentLimit(true)
    }

    private val gyro = ADIS16448_IMU()

    val table = NetworkTableInstance.getDefault().getTable("Drivetrain")

    private const val TICKS_PER_REV = 783
    private const val WHEEL_DIAMETER_INCHES = 5.0
    fun ticksToFeet(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / WHEEL_DIAMETER_INCHES * TICKS_PER_REV

    val gyroAngle: Double
        get() = gyro.angleZ

    private val rampRateCurve = MotionCurve().apply {
        storeValue(Carriage.Pose.INTAKE.lifterHeight, 0.1)
        storeValue(Carriage.Pose.SCALE_HIGH.lifterHeight, 1.0)
    }

    private val leftPosition: Double
        get() = ticksToFeet(leftMotors.getSelectedSensorPosition(0))

    private val rightPosition: Double
        get() = ticksToFeet(rightMotors.getSelectedSensorPosition(0))

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + (hardTurn * 0.8)
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - (hardTurn * 0.8)

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        val rampRate = rampRateCurve.getValue(Carriage.Lifter.height)
        leftMotors.configOpenloopRamp(rampRate, 0)
        rightMotors.configOpenloopRamp(rampRate, 0)

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)
    }

    suspend fun driveDistance(distance: Double, time: Double) {
        val curve = MotionCurve()
        curve.storeValue(0.0, 0.0)
        curve.storeValue(time, distance)
        try {
//            val startLeftPosition = ticksToFeet(leftMotors.getSelectedSensorPosition(0))
//            val startRightPosition = ticksToFeet(rightMotors.getSelectedSensorPosition(0))
            zeroDistance()

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= time }) {
                val t = timer.get()
                val v = curve.getValue(t)
                leftMotors.set(ControlMode.Position, feetToTicks(v))
                rightMotors.set(ControlMode.Position, feetToTicks(v))
            }

            suspendUntil { Math.abs(leftPosition - distance) < 0.1 && Math.abs(rightPosition - distance) < 0.1 }
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
        println("Driving along path ${path2D.name}, duration: ${path2D.duration}")
        path2D.resetDistances()
        try {
            leftMotors.sensorCollection.setQuadraturePosition(0, 0)
            rightMotors.sensorCollection.setQuadraturePosition(0, 0)

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= path2D.easeCurve.length }) {
                val t = timer.get()
                val leftDistance = path2D.getLeftDistance(t)
                val rightDistance = path2D.getRightDistance(t)
                SmartDashboard.putNumber("Left Distance", leftDistance)
                SmartDashboard.putNumber("Right Distance", rightDistance)
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

//        val pEntry = table.getEntry("Position P")
//        val dEntry = table.getEntry("Position D")
//
//        pEntry.setDouble(0.0)
//        dEntry.setDouble(0.0)
//
//        launch {
//            periodic {
//                val p = pEntry.getDouble(0.0)
//                val d = dEntry.getDouble(0.0)
//                leftMotors.config_kP(0, p, 0)
//                leftMotors.config_kD(0, d, 0)
//                rightMotors.config_kP(0, p, 0)
//                rightMotors.config_kD(0, d, 0)
//            }
//        }

        CommandSystem.registerDefaultCommand(this, Command("Drivetrain Default", this) {
            periodic {
                SmartDashboard.putNumberArray("Encoder Distances",
                        arrayOf(ticksToFeet(leftMotors.getSelectedSensorPosition(0)),
                                ticksToFeet(rightMotors.getSelectedSensorPosition(0))))
                SmartDashboard.putNumber("Gyro Angle", gyro.angleZ)
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }
}