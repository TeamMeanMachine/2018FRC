package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.defer
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.IS_COMP_BOT
import org.team2471.frc.powerup.RobotMap
import org.team2471.frc.powerup.commands.returnToIntakePosition
import kotlin.math.absoluteValue
import kotlin.math.max

object Carriage {
    private const val TICKS_PER_INCH = 9437 / 64.25
    private fun ticksToInches(ticks: Double) = ticks / TICKS_PER_INCH
    private fun inchesToTicks(inches: Double) = inches * TICKS_PER_INCH

    private val table = NetworkTableInstance.getDefault().getTable("Carriage")

    @Suppress("LiftReturnOrAssignment")
    private var animationTime = 0.0
        set(value) = when {
            value <= 0.0 -> field = 0.0
            value >= max(Arm.curve.length, Lifter.curve.length) -> field = max(Arm.curve.length, Lifter.curve.length)
            else -> field = value
        }

    init {
        Lifter
        Arm
        CommandSystem.registerDefaultCommand(this, Command("Carriage Default", this) {
            var previousTime = Timer.getFPGATimestamp()
            var prevReleasing = false
            try {
                periodic {
                    val releaseClamp = CoDriver.release
                    Arm.isClamping = !releaseClamp
                    val spit = CoDriver.spitSpeed

                    Arm.intake = if (Arm.hasCube && spit == 0.0) 0.2 else -spit

                    val releasing = releaseClamp || spit != 0.0
                    if (!releasing && prevReleasing && Arm.angle > 150.0) returnToIntakePosition.launch()
                    prevReleasing = releasing

                    val leftStick = CoDriver.leftStickUpDown

                    val currentTime = Timer.getFPGATimestamp()
                    val deltaTime = currentTime - previousTime
                    adjustAnimationTime(deltaTime * leftStick)
                    previousTime = currentTime

                    SmartDashboard.putNumberArray("Amperages", Lifter.amperages)
                    Lifter.isLowGear = false
                    Lifter.isBraking = false
                }
            } finally {
                Arm.isClamping = true
                Arm.intake = 0.0
            }
        })
    }

    enum class Pose(val lifterHeight: Double, val armAngle: Double) {
        INTAKE(6.0, 0.0),
        CRITICAL_JUNCTION(24.0, 110.0),
        SCALE_LOW(24.0, 185.0),
        SCALE_MED(33.0, 185.0),
        SCALE_HIGH(44.0, 185.0),
        CARRY(10.0, 0.0),
        SWITCH(32.0, 15.0),
        CLIMB(58.0, 0.0),
        CLIMB_ACQUIRE_RUNG(26.0, 0.0),
        FACE_THE_BOSS(3.0, 0.0),
        STARTING_POSITION(6.0, 110.0),
    }

    fun adjustAnimationTime(dt: Double) {
        animationTime += dt

        Lifter.heightSetpoint = Lifter.curve.getValue(animationTime)
        Arm.setpoint = Arm.curve.getValue(animationTime)
    }

    fun setAnimation(pose: Pose) {
        Lifter.curve = MotionCurve()
        Arm.curve = MotionCurve()
        Lifter.curve.storeValue(0.0, Lifter.height)
        Lifter.curve.storeValue(1.5, pose.lifterHeight)

        Arm.curve.storeValue(0.0, Arm.angle)
        Arm.curve.storeValue(1.5, pose.armAngle)

        animationTime = 0.0
    }

    suspend fun animateToPose(pose: Pose) {
        setAnimation(pose)

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        Lifter.isBraking = false
        try {
            periodic(condition = { previousTime < Lifter.curve.length }) {
                val time = timer.get()
                adjustAnimationTime(time - previousTime)

                previousTime = time
            }
        } finally {
            Lifter.isBraking = true
            Lifter.stop()
        }
    }

    object Lifter {
        private val motors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
            setSelectedSensorPosition(0, 0, 10)
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(350, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            configPeakOutputForward(1.0, 10)
            configPeakOutputReverse(-1.0, 10)
            configClosedloopRamp(0.1, 10)
            config_kP(0, 1.0, 10)
            config_kI(0, 0.0, 10)
            config_kD(0, 0.3, 10)
            config_kF(0, 0.0, 10)
            inverted = true
            setSensorPhase(true)
        } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(350, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(350, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(350, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            inverted = true
        }

        private val discBrake = Solenoid(RobotMap.Solenoids.BRAKE)

        private val shifter = Solenoid(RobotMap.Solenoids.CARRIAGE_SHIFT)

        private val table = Carriage.table.getSubTable("Lifter")

        init {
            launch {
                val heightEntry = table.getEntry("Height")
                periodic(100) {
                    // don't run the compressor when the carriage exceeds 3V
                    if (!RobotState.isAutonomous()) {
                        RobotMap.compressor.closedLoopControl = motors.motorOutputVoltage.absoluteValue < 3.0
                    }
                    heightEntry.setDouble(height)
                }
            }
        }

        private const val ANIMATION_LENGTH = 1.5

        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.lifterHeight)
            storeValue(ANIMATION_LENGTH, Pose.SCALE_LOW.lifterHeight)
        }

        val height: Double
            get() = ticksToInches(motors.getSelectedSensorPosition(0).toDouble())

        var heightSetpoint: Double = height
            set(value) {
                motors.set(ControlMode.Position, inchesToTicks(value))
                field = value
            }

        var heightRawSpeed: Double = 0.0
            set(value) {
                motors.set(ControlMode.PercentOutput, value)
                field = value
            }

        val heightError = height - heightSetpoint

        var isLowGear: Boolean
            get() = shifter.get()
            set(value) = shifter.set(value)

        var isBraking: Boolean
            get() = !discBrake.get()
            set(value) = discBrake.set(!value)

        val amperages: DoubleArray
            get() = doubleArrayOf(RobotMap.pdp.getCurrent(RobotMap.Talons.ELEVATOR_MOTOR_1),
                    RobotMap.pdp.getCurrent(RobotMap.Talons.ELEVATOR_MOTOR_2),
                    RobotMap.pdp.getCurrent(RobotMap.Talons.ELEVATOR_MOTOR_3),
                    RobotMap.pdp.getCurrent(RobotMap.Talons.ELEVATOR_MOTOR_4))

        val meanAmperage: Double
            get() = amperages.average()

        fun stop() = motors.neutralOutput()

        fun zero() {
            motors.setSelectedSensorPosition(0, 0, 10)
        }
    }

    object Arm {
        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        private val motors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            config_kP(0, 20.0, 10)
            config_kI(0, 0.0, 10)
            config_kD(0, 0.0, 10)
            config_kF(0, 0.0, 10)

            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(300, 10)
            enableCurrentLimit(true)
            setSensorPhase(false)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ARM_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(300, 10)
            enableCurrentLimit(true)
            inverted = true
        }

        private val table = Carriage.table.getSubTable("Arm")

        private val intakeMotorLeft = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            inverted = true
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val cubeSensor = AnalogInput(3)

        @Suppress("ConstantConditionIf")
        private val cubeSensorTriggered: Boolean
            get() = if (IS_COMP_BOT) cubeSensor.voltage > 0.15
            else cubeSensor.voltage < 0.15

        init {
            launch {
                val angleEntry = table.getEntry("Angle")

                periodic(40) {
                    if (cubeSensorTriggered) {
                        hasCube = true
                    } else if (!isClamping || intakeMotorLeft.motorOutputPercent < -0.1) {
                        hasCube = false
                    }
                    CoDriver.passiveRumble = if (hasCube) .15 else 0.0

                    angleEntry.setDouble(angle)
                }
            }
        }

        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.armAngle)
            storeValue(1.5, Pose.SCALE_LOW.armAngle)
        }

        var hasCube = false
            private set

        var isClamping: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        val angle: Double
            get() = ticksToDegrees(motors.getSelectedSensorPosition(0).toDouble())

        var setpoint: Double = angle
            set(value) {
                motors.set(ControlMode.Position, degreesToTicks(value))
                field = value
            }

        val error get() = angle - setpoint

        var intake: Double
            get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
            set(speed) {
                intakeMotorLeft.set(ControlMode.PercentOutput, speed)
                intakeMotorRight.set(ControlMode.PercentOutput, speed)
            }

        private const val ARM_TICKS_PER_DEGREE = 20.0 / 9.0
        private const val ARM_OFFSET_NATIVE = -720.0
        private fun ticksToDegrees(nativeUnits: Double): Double = (nativeUnits - ARM_OFFSET_NATIVE) / ARM_TICKS_PER_DEGREE
        fun degreesToTicks(degrees: Double): Double = degrees * ARM_TICKS_PER_DEGREE + ARM_OFFSET_NATIVE

        fun stop() {
            motors.set(ControlMode.PercentOutput, 0.0)
        }
    }

}