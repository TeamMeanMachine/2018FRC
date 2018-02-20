package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

const val IS_COMP_BOT = true

lateinit var autonomi: Autonomi

class Robot : IterativeRobot() {
    private var hasRunAuto = false

    override fun robotInit() {
        LiveWindow.disableAllTelemetry()

        Drivetrain
        Carriage
        Driver
        AutoChooser

        println("${if (IS_COMP_BOT) "Competition" else "Practice"} mode")
    }

    override fun autonomousInit() {
        hasRunAuto = true

        val loadTime = measureTimeFPGA {
            autonomi = Autonomi.initFromNetworkTables()
        }

        println("Autonomi took $loadTime seconds to load")

        Carriage.Lifter.zero()
        AutoChooser.auto.launch()
    }

    override fun teleopInit() {
        if (!hasRunAuto) runBlocking {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        // println(Carriage.Arm.cubeSensor.voltage)
    }
}
