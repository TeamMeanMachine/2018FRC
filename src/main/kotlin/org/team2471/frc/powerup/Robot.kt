package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

const val IS_COMP_BOT = true

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
        Game.updateGameData()
        hasRunAuto = true

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
