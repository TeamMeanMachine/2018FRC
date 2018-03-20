package org.team2471.frc.powerup.carriage

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
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

object Carriage {
    private const val TICKS_PER_INCH = 9437 / 64.25

    private const val MICRO_ADJUST_RATE = 18.0

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

    var targetPose = Pose.INTAKE
        private set

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

                    Arm.intake = if (spit == 0.0 && Arm.hasCube) 0.2 else -spit

                    val releasing = releaseClamp || spit != 0.0
                    if (!releasing && prevReleasing && Arm.angle > 150.0 && targetPose != Pose.SCALE_FRONT) returnToIntakePosition.launch()
                    prevReleasing = releasing

                    val leftStick = CoDriver.leftStickUpDown

                    val currentTime = Timer.getFPGATimestamp()
                    val deltaTime = currentTime - previousTime
                    previousTime = currentTime


                    if (targetPose == Pose.SWITCH) {
                        val switchOffsetDiff = (CoDriver.microAdjust * MICRO_ADJUST_RATE) * deltaTime
                        switchOffset += switchOffsetDiff
                        Lifter.curve.tailKey.value = targetPose.lifterHeight + switchOffset
                    }

                    if (targetPose.isScale) {
                        val scaleOffsetDiff = (CoDriver.microAdjust * MICRO_ADJUST_RATE) * deltaTime
                        scaleOffset += scaleOffsetDiff

                        Lifter.curve.tailKey.value = targetPose.lifterHeight + scaleOffset

                        SmartDashboard.putNumber("Scale Offset Diff", scaleOffsetDiff)
                        SmartDashboard.putNumber("Scale Offset", scaleOffset)
                    }

                    adjustAnimationTime(deltaTime * leftStick)

                    SmartDashboard.putNumberArray("Lifter Amperages", Lifter.amperages)
                    Lifter.isLowGear = false
                    Lifter.isBraking = false

                    SmartDashboard.putNumber("Arm Amperage", RobotMap.pdp.getCurrent(RobotMap.Talons.ARM_MOTOR_1))
                    SmartDashboard.putBoolean("Low Gear", Lifter.isLowGear)
                    SmartDashboard.putBoolean("Braking", Lifter.isBraking)
                }
            } finally {
                Arm.isClamping = true
            }
        })
    }

    enum class Pose(val lifterHeight: Double, val armAngle: Double) {
        INTAKE(6.0, 0.0),
        CRITICAL_JUNCTION(24.0, 110.0),
        /*      Calibrated at Wilsonville 2018
                SCALE_LOW(22.5, 190.0),
                SCALE_MED(30.0, 185.0),
                SCALE_HIGH(32.0, 185.0),*/
        SCALE_LOW(25.5, 190.0),
        SCALE_MED(36.0, 185.0),
        SCALE_HIGH(38.0, 185.0),
        SCALE_FRONT(190.0,00090.0),
        CARRY(10.0, 0.0),
        SWITCH(26.0, 20.0),
        CLIMB(58.0, 0.0),
        CLIMB_ACQUIRE_RUNG(26.0, 0.0),
        FACE_THE_BOSS(3.0, 0.0),
        STARTING_POSITION(6.0, 110.0);

        val isScale get() = this == SCALE_LOW || this == SCALE_MED || this == SCALE_HIGH || this == SCALE_FRONT
    }

    fun adjustAnimationTime(dt: Double, heightOffset: Double = 0.0) {
        animationTime += dt

        val lifterSetpoint = Lifter.curve.getValue(animationTime) + heightOffset
        val armSetpoint = Arm.curve.getValue(animationTime)
        Lifter.setpoint = lifterSetpoint
        Arm.setpoint = armSetpoint
    }

    val isAnimationCompleted: Boolean
        get() = animationTime >= max(Lifter.curve.length, Arm.curve.length)

    fun setAnimation(pose: Pose, lifterTime: Double = 1.5, armTime: Double = 1.5,
                     lifterTimeOffset: Double = 0.0, armTimeOffset: Double = 0.0, heightOffset: Double = 0.0) {
        targetPose = pose
        Lifter.curve = MotionCurve()
        Arm.curve = MotionCurve()
        Lifter.curve.storeValue(lifterTimeOffset, Lifter.height)
        Lifter.curve.storeValue(lifterTime + lifterTimeOffset,
                min(pose.lifterHeight + heightOffset, Lifter.MAX_HEIGHT))

        Arm.curve.storeValueSlopeAndMagnitude(armTimeOffset, Arm.angle, 0.0, 0.75) //0.5
        Arm.curve.storeValueSlopeAndMagnitude(armTime + armTimeOffset, pose.armAngle, 0.0, 3.0) //2.0


        animationTime = 0.0
    }

    suspend fun animateToPose(pose: Pose, heightOffset: Double = 0.0) {
        val lifterDelta = pose.lifterHeight + heightOffset - Lifter.height
        val armDelta = pose.armAngle - Arm.angle
        val lifterTime = (1.25 / 58.0) * (Math.abs(lifterDelta)) + 0.25
        val armSpeed = if (Arm.hasCube) 1.4 else 1.0
        val armTime = (armSpeed / 180.0) * Math.abs(armDelta) + 0.25
        var lifterTimeOffset = 0.0
        var armTimeOffset = 0.0

        if (lifterDelta > 0.0 && armTime < lifterTime) { // going up and arm time is shorter
            armTimeOffset = lifterTime - armTime
        }
        if (lifterDelta < 0.0 && armTime > lifterTime) { // going down and arm time is longer
            lifterTimeOffset = armTime - lifterTime
        }
        setAnimation(pose, lifterTime, armTime, lifterTimeOffset, armTimeOffset, heightOffset)

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        Lifter.isBraking = false
        periodic(condition = { !isAnimationCompleted }) {
            val t = timer.get()
            adjustAnimationTime(t - previousTime)
            previousTime = t
            SmartDashboard.putNumber("Arm Amperage", RobotMap.pdp.getCurrent(RobotMap.Talons.ARM_MOTOR_1))
        }
    }

    object Lifter {
        private val motors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
            setSelectedSensorPosition(0, 0, 10)
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(100, 10)
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
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(0, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(0, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            inverted = true
        } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(0, 10)
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
                val outputEntry = table.getEntry("Output")
                periodic(100) {
                    // don't run the compressor when the carriage exceeds 3V
                    if (!RobotState.isAutonomous()) {
                        RobotMap.compressor.closedLoopControl = motors.motorOutputVoltage.absoluteValue < 3.0
                    }
                    heightEntry.setDouble(height)

                    if (RobotState.isEnabled())
                        outputEntry.setDouble(motors.motorOutputPercent)
                }
            }
        }

        private const val ANIMATION_LENGTH = 1.5
        const val MAX_HEIGHT = 64.0

        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.lifterHeight)
            storeValue(ANIMATION_LENGTH, Pose.SCALE_LOW.lifterHeight)
        }

        val height: Double
            get() = ticksToInches(motors.getSelectedSensorPosition(0).toDouble())

        var setpoint: Double = height
            set(value) {
                val min = when {
                    Arm.angle > 150.0 -> min(height, Pose.SCALE_LOW.lifterHeight)
                    Arm.angle < 50.0 -> min(height, Pose.INTAKE.lifterHeight)
                    else -> 0.0
                }

                val v = value.coerceIn(min, MAX_HEIGHT)
                motors.set(ControlMode.Position, inchesToTicks(v))
                field = v
            }

        var heightRawSpeed: Double = 0.0
            set(value) {
                motors.set(ControlMode.PercentOutput, value)
                field = value
            }

        val heightError = height - setpoint

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

        private val motor = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            config_kP(0, 13.3, 10)
            config_kI(0, 0.0, 10)
            config_kD(0, 0.0, 10)
            config_kF(0, 0.0, 10)

            configClosedloopRamp(0.0, 10)
            setNeutralMode(NeutralMode.Brake)
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(100, 10)
            enableCurrentLimit(true)
            setSensorPhase(false)
            inverted = true
        }

        val table = Carriage.table.getSubTable("Arm")

        private val intakeMotorLeft = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
            inverted = IS_COMP_BOT
            configOpenloopRamp(0.2, 10)
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(0, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            inverted = !IS_COMP_BOT
            configOpenloopRamp(0.2, 10)
            configContinuousCurrentLimit(25, 10)
            configPeakCurrentLimit(0, 10)
            configPeakCurrentDuration(0, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val minAmperage
            get() = min(intakeMotorLeft.outputCurrent, intakeMotorRight.outputCurrent)

        private val cubeSensor = AnalogInput(2)

        @Suppress("ConstantConditionIf")
        private val cubeSensorTriggered: Boolean
            get() = if (IS_COMP_BOT) cubeSensor.voltage < 0.15
            else cubeSensor.voltage < 0.15

        init {
            launch {
                val angleEntry = table.getEntry("Angle")
                val outputEntry = table.getEntry("Output")
                val velocityEntry = table.getEntry("Velocity")
                val sensorVoltageEntry = table.getEntry("Sensor Voltage")
                val intakeAmperagesEntry = table.getEntry("Intake Amperage")

                val useCubeSensorEntry = table.getEntry("Use Cube Sensor")
                useCubeSensorEntry.setPersistent()

                val cubeTimer = Timer()
                cubeTimer.start()
                periodic(40) {
                    if (!cubeSensorTriggered) {
                        cubeTimer.reset()
                    }

                    usingIntakeSensor = useCubeSensorEntry.getBoolean(true)
                    if ((usingIntakeSensor && cubeTimer.get() > 0.2) || (!usingIntakeSensor && minAmperage > 15)) {
                        hasCube = true
                    } else if (!isClamping || intakeMotorLeft.motorOutputPercent < -0.1) {
                        hasCube = false
                    }
                    CoDriver.passiveRumble = if (hasCube) .12 else 0.0
                    if (RobotState.isEnabled())
                        outputEntry.setNumber(motor.motorOutputPercent)
                    angleEntry.setDouble(angle)

                    sensorVoltageEntry.setDouble(cubeSensor.voltage)
                    velocityEntry.setDouble(velocity)
                    intakeAmperagesEntry.setDoubleArray(doubleArrayOf(intakeMotorLeft.outputCurrent, intakeMotorRight.outputCurrent))
                }
            }
        }

        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.armAngle)
            storeValue(1.5, Pose.SCALE_LOW.armAngle)
        }

        var hasCube = false
            private set

        var usingIntakeSensor = true
            private set

        var isClamping: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        val angle: Double
            get() = ticksToDegrees(motor.getSelectedSensorPosition(0))

        val velocity: Double
            get() = (motor.getSelectedSensorVelocity(0) * 10) / ARM_TICKS_PER_DEGREE

        var setpoint: Double = angle
            set(value) {
                motor.set(ControlMode.Position, degreesToTicks(value))
                field = value
                SmartDashboard.putNumber("Angle Setpoint", value)
            }

        val error get() = angle - setpoint

        var intake: Double
            get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
            set(speed) {
                intakeMotorLeft.set(ControlMode.PercentOutput, speed)
                intakeMotorRight.set(ControlMode.PercentOutput, speed)
            }

        private const val ARM_TICKS_PER_DEGREE = 160.0 / 81.0
        private const val ARM_OFFSET_NATIVE = -720.0
        private fun ticksToDegrees(nativeUnits: Int): Double = (nativeUnits - ARM_OFFSET_NATIVE) / ARM_TICKS_PER_DEGREE
        fun degreesToTicks(degrees: Double): Double = degrees * ARM_TICKS_PER_DEGREE + ARM_OFFSET_NATIVE

        fun hold() {
            setpoint = angle
        }

    }

}