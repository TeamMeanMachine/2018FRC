package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import kotlin.math.absoluteValue
import kotlin.system.measureTimeMillis

object Arm {
    private val offset: Double = 170.0

    private val wristMotors = TalonSRX(RobotMap.Talons.ARM_WRIST_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
        selectProfileSlot(0, 0)
        setNeutralMode(NeutralMode.Brake)
        config_kP(0, 5.0, 10)
        config_kD(0, 2.0, 10)
    } + TalonSRX(RobotMap.Talons.ARM_WRIST_MOTOR_2)

    private val liftingMotors = TalonSRX(RobotMap.Talons.CARRIAGE_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
    } + TalonSRX(RobotMap.Talons.ARM_WRIST_MOTOR_2)

    private var armAngle: Double
        get() = nativeUnitsToDegrees(wristMotors.activeTrajectoryPosition)
        set(value) { /*wristMotors.activeTrajectoryHeading = */ degreesToNativeUnits(value) }

    private var inches: Double
        get() = ticksToInches(liftingMotors.activeTrajectoryPosition)
        set(value) { /*liftingMotors.activeTrajectoryHeading = */ ticksToInches(value.toInt())}

    private val armError get() = armAngle - nativeUnitsToDegrees(wristMotors.activeTrajectoryPosition)//not right

    private val liftError get() = inches - ticksToInches(liftingMotors.activeTrajectoryPosition)

    private val intakeMotor = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
        set(ControlMode.Current, 72.0)
    } + TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT)

    val intakeCurrent get() = RobotMap.pdp.getCurrent(1)

    var intake: Double
        get() = intakeMotor.motorOutputVoltage
        set(speed) = intakeMotor.set(ControlMode.PercentOutput, speed)


    init {
        wristMotors.set(ControlMode.PercentOutput, 0.0)
        CommandSystem.registerDefaultCommand(this, Command("Arm Default", this){
            var armAngle = armAngle
            intake = 0.0
            val animation = if (armAngle < -60 && armAngle > -180){
                Animation(0.0 to currentPose, 0.5 to Pose(45.0, 90.0), 1.0 to Pose.IDLE)
            }
            else {
                Animation(0.0 to currentPose, 0.5 to Pose.IDLE)
            }
            Arm.playAnimation(animation)

            delay(Long.MAX_VALUE)
        })
    }

    suspend fun moveToAngle(angle: Double) {
        val native = degreesToNativeUnits(angle)
        wristMotors.set(ControlMode.Position, native)
        suspendUntil {
            nativeUnitsToDegrees(wristMotors.getClosedLoopError(0)).absoluteValue < 5.0
        }
    }

    private const val TICKS_PER_REV = 783
    private const val SPOOL_DIAMETER_INCHES = 2.0
    private fun ticksToInches(ticks: Int) = ticks.toDouble() / TICKS_PER_REV * SPOOL_DIAMETER_INCHES * Math.PI
    private fun nativeUnitsToDegrees(nativeUnits: Int): Double = (nativeUnits - offset) / (8.0 / 3.0)
    private fun degreesToNativeUnits(angle: Double): Double = (angle) * (8.0 / 3.0) + offset
    suspend fun playAnimation(animation: Animation) {
        val startTime = Timer.getFPGATimestamp()

        periodic(condition = { Timer.getFPGATimestamp() - startTime < animation.length }) {
            val time = Timer.getFPGATimestamp() - startTime
            armAngle = animation.armCurve.getValue(time)
            inches = animation.liftCurve.getValue(time)
        }
        suspendUntil {
            val armError = armError
            val liftError = liftError
            Math.abs(armError) < 3 && Math.abs(liftError) < 3
        }
    }

class Pose(val inches: Double, val armAngle: Double) {
    companion object {
        val IDLE = Pose(0.0, 90.0)
        val INTAKE_POS = Pose(0.0, 0.0)
        val SCALE_POS = Pose(60.0, 180.0)
        val SWITCH_POS = Pose(20.0, 15.0)
        val SCALE_SAFETY = Pose(60.0, 90.0)
    }
}
private val currentPose get() = Pose(inches, armAngle)

class Animation(vararg keyframes: Pair<Double, Pose>) {
    companion object {
        val IDLE_TO_SCALE = Animation(0.0 to Pose.IDLE, 1.0 to Pose.SCALE_POS)
        val SCALE_TO_INTAKE = Animation(0.0 to Pose.SCALE_POS, 0.5 to Pose.SCALE_SAFETY, 1.0 to Pose.INTAKE_POS)
        val SCALE_TO_IDLE = Animation(0.0 to Pose.SCALE_POS, 0.5 to Pose.SCALE_SAFETY, 1.0 to Pose.IDLE)
        val INTAKE_TO_SWITCH = Animation(0.0 to Pose.INTAKE_POS, 1.0 to Pose.SWITCH_POS)
        val IDLE_TO_INTAKE = Animation(0.0 to Pose.IDLE, 1.0 to Pose.INTAKE_POS)
        val IDLE_TO_SWITCH = Animation(0.0 to Pose.IDLE, 1.0 to Pose.SWITCH_POS)
        val INTAKE_TO_IDLE = Animation(0.0 to Pose.INTAKE_POS, 1.0 to Pose.IDLE)
        val SWITCH_TO_IDLE = Animation(0.0 to Pose.SWITCH_POS, 1.0 to Pose.IDLE)
        }

    val armCurve: MotionCurve = MotionCurve().apply {
        keyframes.forEach { (time, pose) ->
            storeValue(time, pose.armAngle)
        }
    }
    val liftCurve: MotionCurve = MotionCurve().apply {
        keyframes.forEach { (time, pose) ->
            storeValue(time, pose.inches)
            }
        }
    val length = liftCurve.length
    }
}
const val AMPERAGE_LIMIT = 21.0
val intakeCubeCommand = Command("Intake Cube", Arm, Intake){
    try {
        Arm.playAnimation(Arm.Animation.IDLE_TO_INTAKE)
        Arm.intake = 1.0
        Intake.clamp = true
        suspendUntil {
            val current = Arm.intakeCurrent
            current > AMPERAGE_LIMIT
        }
        Arm.intake = 0.0
    }finally {
        Arm.playAnimation(Arm.Animation.INTAKE_TO_IDLE)
    }
}

val dropOffToScale = Command("Drop off Cube at Scale", Arm, Intake){
    try {
        Arm.playAnimation(Arm.Animation.IDLE_TO_SCALE)
        Intake.clamp = false
        Arm.intake = 0.0
    }finally {
        Arm.playAnimation(Arm.Animation.SCALE_TO_IDLE)
    }
}