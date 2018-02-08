package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver

import org.team2471.frc.powerup.RobotMap
import kotlin.math.absoluteValue

object Carriage {
    private const val CARRIAGE_SRX_UNITS_TO_INCHES = (1.75 * Math.PI) / 750 // TODO: check for later

    private val motors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
        configContinuousCurrentLimit(20, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(350, 10)
        enableCurrentLimit(true)
    }

    private val position: Double
        get() = motors.getSelectedSensorPosition(0) * CARRIAGE_SRX_UNITS_TO_INCHES

    private var setpoint: Double = position
        set(value) {
            field = value
            motors.set(ControlMode.Position, value / CARRIAGE_SRX_UNITS_TO_INCHES)
        }

    var diskBrake = Solenoid(RobotMap.Solenoids.BRAKE)

    var brake: Boolean = false
        set(value) {
            field = value
            diskBrake.set(brake)
        }

    enum class ShiftSetting {
        FORCE_HIGH,
        FORCE_LOW
    }


    object Arm {
        private const val ARM_SRX_UNITS_TO_DEGREES = 1.0
        private val offset: Double = 170.0

        private val pivotMotors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
        } + TalonSRX(RobotMap.Talons.ARM_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
        }

        private val armClamp = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        private val intakeMotorLeft = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            inverted = true
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
        }

        var clamp: Boolean = false
            set(value) {
                field = value
                armClamp.set(clamp)
            }
        val toggleClampCommand = Command("Toggle Clamp") {
            clamp = !clamp

        }

        private var armAngle: Double
            get() = nativeUnitsToDegrees(pivotMotors.activeTrajectoryPosition)
            set(value) {
                setpoint = degreesToNativeUnits(value)
            }

        private var inches: Double
            get() = ticksToInches(motors.activeTrajectoryPosition)
            set(value) {
                Carriage.setpoint = ticksToInches(value.toInt())
            }

        var intake: Double
            get() = motors.motorOutputVoltage
            set(speed) = motors.set(ControlMode.PercentOutput, speed)

        val intakeCurrent get() = RobotMap.pdp.getCurrent(1)


        private val armError get() = armAngle - nativeUnitsToDegrees(setpoint.toInt())

        private val liftError get() = inches - ticksToInches(setpoint.toInt())

        var speed: Double = 0.0
            set(value) {
                field = value
                motors.set(ControlMode.PercentOutput, value)
            }

        private var leftSpeed: Double
            get() = intakeMotorLeft.motorOutputVoltage / 12
            set(value) = intakeMotorLeft.set(ControlMode.PercentOutput, value)

        private var rightSpeed: Double
            get() = intakeMotorRight.motorOutputVoltage / 12
            set(value) = intakeMotorRight.set(ControlMode.PercentOutput, value)


        init {
            CommandSystem.registerDefaultCommand(this, Command("Elevator Default", this) {
                ShiftSetting.FORCE_LOW
                var armAngle = armAngle
                intake = 0.0
                val animation = if (armAngle < -60 && armAngle > -180) {
                    Animation(0.0 to currentPose, 0.5 to Pose(45.0, 90.0), 1.0 to Pose.IDLE)
                } else {
                    Animation(0.0 to currentPose, 0.5 to Pose.IDLE)
                }
                Arm.playAnimation(animation)
                delay(Long.MAX_VALUE)
                clamp = CoDriver.grab
                speed = CoDriver.updown
                brake = CoDriver.brake
                periodic {
                    if (CoDriver.invertIntake) {
                        leftSpeed = -CoDriver.leftIntake
                        rightSpeed = -CoDriver.rightIntake
                    } else {
                        leftSpeed = CoDriver.leftIntake
                        rightSpeed = CoDriver.rightIntake
                    }
                }
            })
        }

        suspend fun moveToAngle(angle: Double) {
            val native = degreesToNativeUnits(angle)
            pivotMotors.set(ControlMode.Position, native)
            suspendUntil {
                nativeUnitsToDegrees(pivotMotors.getClosedLoopError(0)).absoluteValue < 5.0
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
                val CLIMB = Pose(40.0, 0.0)
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
                val INTAKE_TO_SCALE = Animation(0.0 to Pose.INTAKE_POS, 0.5 to Pose.SCALE_SAFETY, 1.0 to Pose.SCALE_POS)
                val EXTEND_ELEVATOR = Animation(0.0 to Pose.IDLE, 1.0 to Pose.CLIMB)
                val FINISH_CLIMB = Animation(0.0 to Pose.CLIMB, 1.0 to Pose.IDLE)
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

        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        val position: Double
            get() = pivotMotors.getSelectedSensorPosition(0) * ARM_SRX_UNITS_TO_DEGREES

        var setpoint: Double = position
            set(value) {
                field = value
                pivotMotors.set(ControlMode.Position, value / ARM_SRX_UNITS_TO_DEGREES)
            }

        var clawClosed: Boolean
            get() = clawSolenoid.get()
            set(value) = clawSolenoid.set(value)

    }

    const val AMPERAGE_LIMIT = 21.0
    val intakeCubeCommand = Command("Intake Cube", Carriage) {
        try {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.IDLE_TO_INTAKE)
            Carriage.Arm.intake = 1.0
            Carriage.Arm.clamp = true
            suspendUntil {
                val current = Carriage.Arm.intakeCurrent
                current > AMPERAGE_LIMIT
            }
            Carriage.Arm.intake = 0.0
        } finally {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_IDLE)
        }
    }

    val dropOffToScale = Command("Drop off Cube at Scale", Carriage) {
        try {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.IDLE_TO_SCALE)
            Carriage.Arm.clamp = false
        } finally {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.SCALE_TO_IDLE)
        }
    }

    val dropOffToScaleAuto = Command("Drop off Cube at Scale during Auto", Carriage) {
        try {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.IDLE_TO_SCALE)
            Carriage.Arm.clamp = false
        } finally {
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.SCALE_TO_INTAKE)
            Carriage.Arm.intake = 1.0
        }
    }


    val climb = Command("Climb", Carriage){
        try{
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.EXTEND_ELEVATOR)
            ShiftSetting.FORCE_HIGH
            Carriage.Arm.playAnimation(Carriage.Arm.Animation.FINISH_CLIMB)
        }finally {
            ShiftSetting.FORCE_LOW
        }
    }

}