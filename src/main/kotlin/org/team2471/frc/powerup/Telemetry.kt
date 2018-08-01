package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.RobotController
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.periodic

object Telemetry {
    private const val LOG_MOTOR_INFO = false


    private val table = NetworkTableInstance.getDefault().getTable("Telemetry")
    private val tickRateEntry = table.getEntry("Tick Rate")

    private val motorsTable = table.getSubTable("Motors")

    private val motors = ArrayList<Pair<NetworkTable, TalonSRX>>()

    private var lastTime = Timer.getFPGATimestamp()

    private var brownOutCount = 0
    private var jitterCount = 0

    private val timeEntry = table.getEntry("Time")
    private val brownedOutEntry = table.getEntry("Browned Out").apply { setNumber(0) }
    private val brownOutsEntry = table.getEntry("Brown Outs")
    private val jitterCountEntry = table.getEntry("Jitter Count").apply { setNumber(0) }
    private val sysActiveEntry = table.getEntry("System Active")
    private val robotEnabledEntry = table.getEntry("Robot Enabled")

    fun registerMotor(label: String, motor: TalonSRX) = motors.add(motorsTable.getSubTable(label) to motor)

    fun tick() {
        val time = Timer.getFPGATimestamp()
        tickRateEntry.setDouble(time - lastTime)
        lastTime = time

        timeEntry.setDouble(Timer.getFPGATimestamp())

        if (LOG_MOTOR_INFO) motors.forEach { (table, motor) ->
            table.getEntry("Output").setDouble(motor.motorOutputPercent)
            table.getEntry("Bus Voltage").setDouble(motor.busVoltage)
            table.getEntry("Output Current").setDouble(motor.outputCurrent)
        }

        val brownedOut = RobotController.isBrownedOut()
        brownedOutEntry.setNumber(if (brownedOut) 1 else 0)
        robotEnabledEntry.setNumber(if (RobotState.isEnabled()) 1 else 0)
        if (brownedOut) {
            brownOutCount++
            DriverStation.reportWarning("PDP Browned Out, Count: $brownOutCount", false)
            brownOutsEntry.setNumber(brownOutCount)
        }

        val jitter = RobotController.isSysActive() && !brownedOut
        sysActiveEntry.setNumber(if (jitter) 1 else 0)
        if (!jitter && RobotState.isEnabled()) {
            jitterCount++
            DriverStation.reportWarning("Jitter Detected, Count: $jitterCount", false)
            jitterCountEntry.setNumber(jitterCount)
        }

        if (RobotMap.Solenoids.discBrake.pcmSolenoidVoltageFault) {
            println("Disk brake faults")
        }
        if (RobotMap.Solenoids.shifter.pcmSolenoidVoltageFault) {
            println("Lifter shift voltage fault")
        }
    }
}