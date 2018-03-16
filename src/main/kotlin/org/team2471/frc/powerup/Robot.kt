package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

const val IS_COMP_BOT = false

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
        LEDController.alliance = Game.alliance
        hasRunAuto = true
        LEDController.state = TheatreState
        Carriage.Lifter.zero()
        AutoChooser.auto.launch()
    }

    override fun teleopInit() {
        if (!hasRunAuto) runBlocking {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }
        LEDController.state = FireState
        CommandSystem.initDefaultCommands()
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        SmartDashboard.putNumber("Match Time", (Game.matchTime - 3.0).coerceAtLeast(-1.0))
    }
}
