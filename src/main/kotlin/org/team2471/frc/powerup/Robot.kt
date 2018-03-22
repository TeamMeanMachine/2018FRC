package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.RobotController
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Pose
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


        Game.updateGameData()
        val gameAlliance = Game.alliance
        println("RobotInit alliance: $gameAlliance")
        LEDController.alliance = gameAlliance
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
            Carriage.animateToPose( Pose.INTAKE)
        }
        LEDController.state = FireState
        Drivetrain.zeroEncoders()
        CommandSystem.initDefaultCommands()
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        SmartDashboard.putNumber("Match Time", (Game.matchTime - 3.0).coerceAtLeast(-1.0))

        if (RobotController.isBrownedOut()) DriverStation.reportWarning("PDP Browned Out", false)
        if (!RobotController.getEnabled3V3()) DriverStation.reportWarning("3V3 Disabled", false)
        if (!RobotController.getEnabled5V()) DriverStation.reportWarning("5V Disabled", false)
        if (!RobotController.getEnabled6V()) DriverStation.reportWarning("6V Disabled", false)
    }

    override fun disabledInit() {
        LEDController.state = IdleState
    }

    override fun testInit() {
        Drivetrain.calibrateGyro()
    }
}
