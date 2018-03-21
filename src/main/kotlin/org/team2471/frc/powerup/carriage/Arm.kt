package org.team2471.frc.powerup.carriage

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.IS_COMP_BOT
import org.team2471.frc.powerup.RobotMap
import kotlin.math.min

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
