package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.drivetrain.Drivetrain

const val IS_COMP_BOT = false

class Robot : IterativeRobot() {
    private var hasRunAuto = false

    override fun robotInit() {
        LiveWindow.disableAllTelemetry()

        Drivetrain
        Carriage
        Driver
        AutoChooser

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2") // use 2 threads in CommonPool

        println("${if (IS_COMP_BOT) "Competition" else "Practice"} mode")
        SmartDashboard.putNumber("Test Throttle", 1.0)
    }

    override fun autonomousInit() {
        hasRunAuto = true
        Game.updateGameData()
        LEDController.alliance = Game.alliance
        LEDController.state = TheatreState
        Drivetrain.zeroGyro()
        AutoChooser.auto.launch()
    }

    override fun teleopInit() {
        if (!hasRunAuto) runBlocking {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }
        LEDController.state = FireState
        Drivetrain.zeroEncoders()
        CommandSystem.initDefaultCommands()
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        SmartDashboard.putNumber("Match Time", (Game.matchTime - 3.0).coerceAtLeast(-1.0))
    }

    override fun disabledInit() {
        LEDController.state = IdleState
    }

    override fun testInit() {
        Drivetrain.calibrateGyro()
    }
}
