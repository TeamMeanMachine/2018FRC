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
    private val table = NetworkTableInstance.getDefault().getTable("Telemetry")
    private val tickRateEntry = table.getEntry("Tick Rate")

    private val motorsTable = table.getSubTable("Motors")

    private val motors = ArrayList<Pair<NetworkTable, TalonSRX>>()

    private var lastTime = Timer.getFPGATimestamp()

    fun tick() {
        val time = Timer.getFPGATimestamp()
        tickRateEntry.setDouble(time - lastTime)
        lastTime = time
    }

    fun registerMotor(label: String, motor: TalonSRX) = motors.add(motorsTable.getSubTable(label) to motor)

    fun start() {

        launch {
            var brownOutCount = 0
            var jitterCount = 0

            val timeEntry = table.getEntry("Time")
            val brownedOutEntry = table.getEntry("Browned Out")
            val brownOutsEntry = table.getEntry("Brown Outs")
            val jitterCountEntry = table.getEntry("Jitter Count")
            val sysActiveEntry = table.getEntry("System Active")
            val robotEnabledEntry = table.getEntry("Robot Enabled")
            brownOutsEntry.setNumber(0)
            jitterCountEntry.setNumber(0)

            periodic(5) {
                timeEntry.setDouble(Timer.getFPGATimestamp())
                motors.forEach { (table, motor) ->
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
            }
        }
    }
}