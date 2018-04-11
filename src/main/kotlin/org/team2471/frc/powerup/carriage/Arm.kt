package org.team2471.frc.powerup.carriage

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.DemandType
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.math.average
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.IS_COMP_BOT
import org.team2471.frc.powerup.RobotMap
import org.team2471.frc.powerup.Telemetry
import kotlin.math.min

object Arm {
    private val clawSolenoid = Solenoid(RobotMap.Solenoids.INTAKE_CLAW)

    private val motor = TalonSRX(RobotMap.Talons.ARM_MOTOR_1).apply {
        configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
        config_kP(0, CarriageConstants.ARM_P, 10)
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
        Telemetry.registerMotor("Arm", this)
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
        Telemetry.registerMotor("Left Intake", this)
    }

    private val intakeMotorRight = TalonSRX(RobotMap.Talons.INTAKE_MOTOR_RIGHT).apply {
        inverted = !IS_COMP_BOT
        configOpenloopRamp(0.2, 10)
        configContinuousCurrentLimit(25, 10)
        configPeakCurrentLimit(0, 10)
        configPeakCurrentDuration(0, 10)
        enableCurrentLimit(true)
        setNeutralMode(NeutralMode.Coast)
        Telemetry.registerMotor("Right Intake", this)
    }

    private val minAmperage
        get() = min(intakeMotorLeft.outputCurrent, intakeMotorRight.outputCurrent)

    private val cubeSensor = AnalogInput(1)

    val detectingCube: Boolean
        get() = Carriage.targetPose == Pose.INTAKE && Carriage.isAnimationCompleted &&
                if (usingIntakeSensor) cubeSensor.voltage < 0.15
                else isClamping && minAmperage > 20

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
            periodic {
                usingIntakeSensor = useCubeSensorEntry.getBoolean(true)
                SmartDashboard.putBoolean("Using Intake Sensor" ,usingIntakeSensor)



                if (!detectingCube) {
                    cubeTimer.reset()
                }

//                if (cubeTimer.get() > 0.15) {
                if (detectingCube && Carriage.targetPose == Pose.INTAKE) {
                    hasCube = true
                } else if ((!isClamping && !detectingCube) || intakeMotorLeft.motorOutputPercent < -0.1) {
                    hasCube = false
                }
                CoDriver.passiveRumble = if (hasCube) .12 else 0.0
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
        get() = (motor.getSelectedSensorVelocity(0) * 10) / CarriageConstants.ARM_TICKS_PER_DEGREE

    var setpoint: Double = angle
        set(value) {
            motor.set(ControlMode.Position, degreesToTicks(value))
            field = value
            SmartDashboard.putNumber("Angle Setpoint", value)
        }

    fun set(position: Double, velocity: Double) = motor.set(ControlMode.Position, degreesToTicks(position),
            DemandType.ArbitraryFeedForward, velocity * CarriageConstants.ARM_VELOCITY_FEED_FORWARD.also {
    })

    val error get() = angle - setpoint

    var intakeSpeed: Double
        get() = average(intakeMotorLeft.motorOutputVoltage, intakeMotorRight.motorOutputVoltage) / 12
        set(speed) {
            intakeMotorLeft.set(ControlMode.PercentOutput, speed)
            intakeMotorRight.set(ControlMode.PercentOutput, speed)
        }

    var rawPower: Double
        get() = motor.motorOutputPercent
        set(value) {
            val angle = angle
            motor.set(ControlMode.PercentOutput, if (angle > 0.0 || angle < Pose.SCALE_LOW.armAngle) value else 0.0)
        }

    private fun ticksToDegrees(nativeUnits: Int): Double = (nativeUnits - CarriageConstants.ARM_OFFSET_NATIVE) / CarriageConstants.ARM_TICKS_PER_DEGREE

    private fun degreesToTicks(degrees: Double): Double = degrees * CarriageConstants.ARM_TICKS_PER_DEGREE + CarriageConstants.ARM_OFFSET_NATIVE

    fun hold() {
        setpoint = angle
    }
}

val testArmCommand = Command("Test Arm Command", Carriage) {
    periodic {
        Arm.rawPower = CoDriver.leftStickUpDown
    }
}

