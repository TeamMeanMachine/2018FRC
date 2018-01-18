package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import kotlin.math.absoluteValue

object Arm {
    private val offset: Double = 170.0

    private val wristMotors = TalonSRX(RobotMap.Talons.ARM_WRIST_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
        selectProfileSlot(0, 0)
        setNeutralMode(NeutralMode.Brake)
        config_kP(0, 5.0, 10)
        config_kD(0, 2.0, 10)
    } + TalonSRX(RobotMap.Talons.ARM_WRIST_MOTOR_2)

    init {
        wristMotors.set(ControlMode.PercentOutput, 0.0)
    }

    suspend fun moveToAngle(angle: Double) {
        val native = degreesToNativeUnits(angle)
        wristMotors.set(ControlMode.Position, native)
        suspendUntil {
            nativeUnitsToDegrees(wristMotors.getClosedLoopError(0)).absoluteValue < 5.0
        }
    }

    private fun nativeUnitsToDegrees(nativeUnits: Int): Double = (nativeUnits - offset) / (8.0 / 3.0)
    private fun degreesToNativeUnits(angle: Double): Double = (angle) * (8.0 / 3.0) + offset
}