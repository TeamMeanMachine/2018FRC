package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.NonCancellable.start
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.RobotMap

object Drivetrain {
    private val leftMotors = TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1).apply {
        setNeutralMode(NeutralMode.Brake)
    } + TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
    }
    private val rightMotors = TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    } + TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2).apply {
        setNeutralMode(NeutralMode.Brake)
        inverted = true
    }

    fun drive(throttle: Double, softTurn: Double, hardTurn: Double) {
        var leftPower = throttle + (softTurn * Math.abs(throttle)) + hardTurn
        var rightPower = throttle - (softTurn * Math.abs(throttle)) - hardTurn

        val maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower))
        if (maxPower > 1) {
            leftPower /= maxPower
            rightPower /= maxPower
        }

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

    init {
        CommandSystem.registerDefaultCommand(this, Command("Drivetrain Default", this) {
            periodic {
                drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
            }
        })
    }
}