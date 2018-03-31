package org.team2471.frc.powerup.carriage

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Solenoid
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.plus
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.RobotMap
import org.team2471.frc.powerup.Telemetry
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

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
        configAllowableClosedloopError(0, 0, 10)
        config_kP(0, 1.0, 10)
        config_kI(0, 0.0, 10)
        config_kD(0, 0.3, 10)
        config_kF(0, 0.0, 10)
        inverted = true
        setSensorPhase(true)
        Telemetry.registerMotor("Lifter Master", this)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_2).apply {
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(0, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
        Telemetry.registerMotor("Lifter Slave 1", this)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_3).apply {
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(0, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
        Telemetry.registerMotor("Lifter Slave 2", this)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_MOTOR_4).apply {
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(0, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Brake)
        inverted = true
        Telemetry.registerMotor("Lifter Slave 3", this)
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
                RobotMap.compressor.closedLoopControl = !RobotState.isAutonomous() && motors.motorOutputPercent < 0.1
                heightEntry.setDouble(height)

                if (RobotState.isEnabled())
                    outputEntry.setDouble(motors.motorOutputPercent)
            }
        }
    }

    var curve = MotionCurve().apply {
        storeValue(0.0, Pose.INTAKE.lifterHeight)
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

            val v = value.coerceIn(min, CarriageConstants.LIFTER_MAX_HEIGHT)
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


    private fun ticksToInches(ticks: Double) = ticks / CarriageConstants.LIFTER_TICKS_PER_INCH

    private fun inchesToTicks(inches: Double) = inches * CarriageConstants.LIFTER_TICKS_PER_INCH
}