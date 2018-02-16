package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.*
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.CoDriver
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
        CommandSystem.registerDefaultCommand(this, Command("Carriage Default", this) {
            var previousTime = Timer.getFPGATimestamp()
            var prevReleasing = false
            try {
                periodic {
                    val releaseClamp = CoDriver.release
                    Arm.clamp = !releaseClamp
                    val spit = CoDriver.spitSpeed
                    Arm.intake = if (spit == 0.0) 0.2 else -spit

                    val releasing = releaseClamp || spit != 0.0
                    if (!releasing && prevReleasing) returnToIntakePosition.launch()
                    prevReleasing = releasing

                    SmartDashboard.putNumber("Height Number: ", Lifter.height)
                    SmartDashboard.putNumber("Arm Angle: ", Arm.angle)

                    val leftStick = CoDriver.leftStickUpDown

                    val currentTime = Timer.getFPGATimestamp()
                    val deltaTime = currentTime - previousTime
                    adjustAnimationTime(deltaTime * leftStick)
                    previousTime = currentTime

                    if (RobotState.isEnabled())
                        println("height setpoint: ${Lifter.heightSetpoint} | arm setpoint: ${Arm.setpoint}")

                    SmartDashboard.putNumberArray("Amperages", Lifter.amperages)
                    Lifter.isLowGear = false
                    Lifter.isBraking = false
                }
            } finally {
                Arm.clamp = true
                Arm.intake = 0.0

            }
        })
    }

    enum class Pose(val inches: Double, val armAngle: Double) {
        INTAKE(6.0, 0.0),
        CRITICAL_JUNCTION(24.0, 110.0),
        SCALE_LOW(24.0, 190.0),
        SCALE_MED(33.0, 190.0),
        SCALE_HIGH(44.0, 190.0),
        IDLE(0.0, 90.0),
        SWITCH(20.0, 20.0),
        SCALE_SAFETY(60.0, 90.0),
        CLIMB(40.0, 0.0)
    }

    fun adjustAnimationTime(dt: Double) {
        animationTime += dt

        Lifter.heightSetpoint = Lifter.curve.getValue(animationTime)
        Arm.setpoint = Arm.curve.getValue(animationTime)
        if (RobotState.isEnabled()) {
            println("Lifter: ${Lifter.heightSetpoint} Arm: ${Arm.setpoint}")
            println("Animation Time: $animationTime")
        }

    }

    suspend fun animateToPose(height: Double, angle: Double) {
        Lifter.curve = MotionCurve()
        Arm.curve = MotionCurve()
        Lifter.curve.storeValue(0.0, Lifter.height)
        Lifter.curve.storeValue(1.5, height)

        Arm.curve.storeValue(0.0, Arm.angle)
        Arm.curve.storeValue(1.5, angle)

        animationTime = 0.0

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic(condition = {
            println("Previous time: $previousTime, length: ${Lifter.curve.length}")
            previousTime < Lifter.curve.length
        }) {
            val time = timer.get()
            adjustAnimationTime(time - previousTime)

            previousTime = time
        }
    }

    object Lifter {
        private val liftMotors = TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)
            setSelectedSensorPosition(0, 0, 10)
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(350, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Brake)
            configPeakOutputForward(1.0, 10)
            configPeakOutputReverse(-1.0, 10)
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

        private val magnetSensor = DigitalInput(0)

        init {
            launch {
                periodic(100) {
                    // don't run the compressor when the carriage exceeds 3V
                    RobotMap.compressor.closedLoopControl = liftMotors.motorOutputVoltage.absoluteValue < 3.0
                }
            }
        }

        private const val ANIMATION_LENGTH = 1.5

        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.inches)
            storeValue(ANIMATION_LENGTH, Pose.SCALE_LOW.inches)
        }

        val height: Double
            get() = ticksToInches(liftMotors.getSelectedSensorPosition(0).toDouble())

        var heightSetpoint: Double = height
            set(value) {
                liftMotors.set(ControlMode.Position, inchesToTicks(value))
                field = value
            }

        var heightRawSpeed: Double = 0.0
            set(value) {
                liftMotors.set(ControlMode.PercentOutput, value)
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

        fun zero() {
            liftMotors.setSelectedSensorPosition(0, 0, 10)
        }
    }

    object Arm {
        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        val armMotors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
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

        val table = Carriage.table.getSubTable("Arm")

        init {
            launch {
                val rawEntry = table.getEntry("Raw Angle")
                val normalEntry = table.getEntry("Angle")
                val errorEntry = table.getEntry("Error")
                val setpointError = table.getEntry("Setpoint")
                periodic {
                    val raw = armMotors.getSelectedSensorPosition(0).toDouble()
                    rawEntry.setDouble(raw)
                    normalEntry.setDouble(ticksToDegrees(raw))
//                    errorEntry.setDouble(armPID.error)
//                    setpointError.setDouble(armPID.setpoint)
                }
            }
        }

        private val intakeMotorLeft = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_LEFT).apply {
            inverted = true
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(15, 10)
            configPeakCurrentDuration(200, 10)
            enableCurrentLimit(true)
            setNeutralMode(NeutralMode.Coast)
        }

        private val cubeSensor = AnalogInput(3)


        var curve = MotionCurve().apply {
            storeValue(0.0, Pose.INTAKE.armAngle)
            storeValue(1.5, Pose.SCALE_LOW.armAngle)
        }

        val hasCube: Boolean
            get() = cubeSensor.voltage < 0.15

        var clamp: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        val angle: Double
            get() = ticksToDegrees(armMotors.getSelectedSensorPosition(0).toDouble())

        var setpoint: Double = angle
            set(value) {
                armMotors.set(ControlMode.Position, degreesToTicks(value))
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
        private const val ARM_OFFSET_NATIVE = -730.0
        private fun ticksToDegrees(nativeUnits: Double): Double = (nativeUnits - ARM_OFFSET_NATIVE) / ARM_TICKS_PER_DEGREE
        fun degreesToTicks(degrees: Double): Double = degrees * ARM_TICKS_PER_DEGREE + ARM_OFFSET_NATIVE

        var clawClosed: Boolean
            get() = clawSolenoid.get()
            set(value) = clawSolenoid.set(value)
    }

}