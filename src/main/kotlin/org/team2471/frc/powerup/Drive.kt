package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.Timer
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.motion_profiling.Path2D
import java.lang.Math.copySign

object Drive {
    private val shifter = Solenoid(0)

    private const val LEFT_POWER_OFFSET = 0.0
    private const val RIGHT_POWER_OFFSET = 0.0

    private var position = 0.0

    val leftMotors = TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setSensorPhase(true)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.5, 10)
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_3).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.DRIVE_LEFT_MOTOR_4).apply {
        setNeutralMode(NeutralMode.Brake)
    }

    val rightMotors = TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_3).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        setSensorPhase(true)
        config_kP(0, 2.0, 10)
        config_kD(0, 0.5, 10)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_1).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.DRIVE_RIGHT_MOTOR_4).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    }

    init {
        leftMotors.sensorCollection.setQuadraturePosition(0, 0)
        rightMotors.sensorCollection.setQuadraturePosition(0, 0)

        CommandSystem.registerDefaultCommand(this, Command("Drive Default", this) {
            periodic {
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double, experimentalOffset: Boolean = false) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

        if (experimentalOffset) {
            leftPower = (leftPower + copySign(LEFT_POWER_OFFSET, leftPower)) / (1 - LEFT_POWER_OFFSET)
            rightPower = (rightPower + copySign(RIGHT_POWER_OFFSET, rightPower)) / (1 - RIGHT_POWER_OFFSET)
        }

        shifter.set(true)

        leftMotors.set(ControlMode.PercentOutput, leftPower)
        rightMotors.set(ControlMode.PercentOutput, rightPower)
    }

    fun driveRaw(leftPower: Double, rightPower: Double) {
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
            leftMotors.sensorCollection.setQuadraturePosition(0, 0)
            rightMotors.sensorCollection.setQuadraturePosition(0, 0)

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= time }) {
                val t = timer.get()
                leftMotors.set(ControlMode.Position, feetToTicks(curve.getValue(t)))
                rightMotors.set(ControlMode.Position, feetToTicks(curve.getValue(t)))
            }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }
    suspend fun driveAlongPath(path2D: Path2D) {
        var leftDistance = 0.0
        var rightDistance = 0.0
        try {
            leftMotors.sensorCollection.setQuadraturePosition(0, 0)
            rightMotors.sensorCollection.setQuadraturePosition(0, 0)

            val timer = Timer().apply { start() }
            periodic(condition = { timer.get() <= path2D.easeCurve.length }) {
                val t = timer.get()
                leftDistance += path2D.getLeftPositionDelta(t)
                rightDistance += path2D.getRightPositionDelta(t)
                leftMotors.set(ControlMode.Position, feetToTicks(leftDistance))
                rightMotors.set(ControlMode.Position, feetToTicks(rightDistance))
            }
        } finally {
            leftMotors.neutralOutput()
            rightMotors.neutralOutput()
        }
    }

    private const val TICKS_PER_REV = 783
    private const val WHEEL_DIAMETER_INCHES = 6.0
    fun ticksToFeet(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * WHEEL_DIAMETER_INCHES * Math.PI / 12.0
    fun feetToTicks(feet: Double) = feet * 12.0 / Math.PI / WHEEL_DIAMETER_INCHES * TICKS_PER_REV
}
