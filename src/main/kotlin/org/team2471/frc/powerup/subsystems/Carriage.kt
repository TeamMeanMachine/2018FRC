package org.team2471.frc.powerup.subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.*
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.PIDController
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.math.clamp
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.RobotMap

object Carriage {
    private const val TICKS_PER_INCH = 7550.0 / 64.25
    private fun ticksToInches(ticks: Double) = ticks / TICKS_PER_INCH
    private fun inchesToTicks(inches: Double) = inches * TICKS_PER_INCH

    private const val FEED_FORWARD_UP = 1.705

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
        config_kP(0, 0.5, 10)
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

    private val table = NetworkTableInstance.getDefault().getTable("Carriage")

    val height: Double
        get() = ticksToInches(liftMotors.getSelectedSensorPosition(0).toDouble())

    var heightSetpoint: Double = height
        set(value) {
            liftMotors.set(ControlMode.Position, inchesToTicks(value))
            field = value
        }

    val heightError = height - heightSetpoint

    var isLowGear: Boolean
        get() = shifter.get()
        set(value) = shifter.set(value)

    var isBraking: Boolean
        get() = !discBrake.get()
        set(value) = discBrake.set(!value)

    val atMaxHeight: Boolean
        get() = magnetSensor.get()

    val amperage: Double
        get() = liftMotors.outputCurrent

    init {
        CommandSystem.registerDefaultCommand(this, Command("Carriage Default", this) {
            periodic {
                val rightStick = CoDriver.rightStickUpDown
                Arm.setpoint = rightStick * 45 + 90

                val leftStick = CoDriver.leftStickUpDown
                heightSetpoint = leftStick * 12 + 0

                isLowGear = false
                isBraking = false
            }
        })
    }

    suspend fun moveToHeight(height: Double) {
        heightSetpoint = height
        try {
            isBraking = false
            isLowGear = false
            suspendUntil { Math.abs(this.height - height) < 2.5 }
        } finally {
            isBraking = true
            isLowGear = true
        }
    }

    suspend fun moveToPose(pose: Pose) {
        val safeRange = 0.0..110.0
        Arm.setpoint = safeRange.clamp(Arm.angle)
        suspendUntil { Arm.angle in safeRange }
        heightSetpoint = pose.inches
        suspendUntil { /*Far enough to move the arm to position */ false }
        Arm.setpoint = pose.armAngle
        suspendUntil { /* done */ false }
    }

    suspend fun zero() {
        try {
            periodic(condition = { liftMotors.outputCurrent < 5.0 }) {
                liftMotors.set(ControlMode.PercentOutput, -0.2)
            }
        } finally {
            liftMotors.neutralOutput()
        }

        liftMotors.setSelectedSensorPosition(0, 0, 10)
    }

    class Pose(val inches: Double, val armAngle: Double) {
        companion object {
            val INTAKE = Pose(0.0, 0.0)
            val CRITICAL_JUNCTION = Pose(24.0, 110.0)
            val SCALE = Pose(60.0, 180.0)
            val IDLE = Pose(0.0, 90.0)
            val SWITCH = Pose(20.0, 0.0)
            val SCALE_SAFETY = Pose(60.0, 90.0)
            val CLIMB = Pose(40.0, 0.0)
        }
    }

    class Animation(vararg keyframes: Pair<Double, Pose>) {
        companion object {
            val INTAKE_TO_SCALE = Animation(0.0 to Pose.INTAKE, 5.0 to Pose.CRITICAL_JUNCTION, 10.0 to Pose.SCALE)
            val INITIAL_TEST = Animation(0.0 to Pose.INTAKE, 2.0 to Pose.SWITCH)
        }

        val lifterCurve: MotionCurve = MotionCurve().apply {
            keyframes.forEach { (time, pose) ->
                storeValue(time, pose.inches)
            }
        }

        val armCurve: MotionCurve = MotionCurve().apply {
            keyframes.forEach { (time, pose) ->
                storeValue(time, pose.armAngle)
            }
        }

        val length = lifterCurve.length
    }

    private val currentPose get() = Pose(height, Arm.angle)

    suspend fun playAnimation(animation: Animation) {
        val timer = Timer()
        timer.reset()
        try {
            isLowGear = false
            isBraking = false

            var time = 0.0
            periodic(condition = { time = timer.get();time < animation.length }) {
                heightSetpoint = animation.lifterCurve.getValue(time)
                Arm.setpoint = animation.armCurve.getValue(time)
                SmartDashboard.putNumber("Elevator Setpoint", heightSetpoint)
                SmartDashboard.putNumber("Arm Setpoint", Arm.setpoint)
            }

            suspendUntil {
                Math.abs(heightError) < 1 && Math.abs(Arm.error) < 3
            }
        } finally {
            isLowGear = true
            isBraking = true
        }
    }

    object Arm {
        private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

        private val armMotors = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            config_kP(0, 5.0, 10)
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
                    errorEntry.setDouble(armPID.error)
                    setpointError.setDouble(armPID.setpoint)
                }
            }
        }

        val armPID = PIDController(.04, 0.0, 0.01, 0.0, {
            ticksToDegrees(armMotors.getSelectedSensorPosition(0).toDouble())
        }, { armMotors.set(ControlMode.PercentOutput, it) }, { armMotors.motorOutputPercent }).apply {
            isEnabled = true
            SmartDashboard.putData("Arm PID", this)
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

        val cubeSensor = AnalogInput(3)

        val hasCube: Boolean
            get() = cubeSensor.voltage < 0.15

        var clamp: Boolean
            get() = !clawSolenoid.get()
            set(value) = clawSolenoid.set(!value)

        val angle: Double
            get() = ticksToDegrees(armMotors.getSelectedSensorPosition(0).toDouble())

        var setpoint: Double
            get() = armPID.setpoint
            set(value) {
                armPID.setpoint = value
            }
        val error = angle - setpoint

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