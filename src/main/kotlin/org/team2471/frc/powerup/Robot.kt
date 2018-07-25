package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.NeutralMode
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.runBlocking
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Pose
import org.team2471.frc.powerup.drivetrain.Drivetrain

const val IS_COMP_BOT = true

class Robot : IterativeRobot() {
    private var hasRunAuto = false

    override fun robotInit() {
        //CameraStream.isEnabled = true
        Game.updateGameData()

        println("${if (IS_COMP_BOT) "Competition" else "Practice"} mode")

        Drivetrain
        Carriage
        Driver
        AutoChooser

        LEDController.state = IdleState
        val gameAlliance = Game.alliance
        LEDController.alliance = gameAlliance

        SmartDashboard.putNumber("Test Throttle", 1.0)
        SmartDashboard.putBoolean("Callibrate Gyro", false)
        LiveWindow.disableAllTelemetry()
    }

    override fun robotPeriodic() {
        val eventMapperTime = measureTimeFPGA {
            EventMapper.tick()
        }

        SmartDashboard.putNumber("Event Mapper Time", eventMapperTime)
        Telemetry.tick()
        SmartDashboard.putNumber("Match Time", (Game.matchTime - 3.0).coerceAtLeast(-1.0))
    }

    override fun autonomousInit() {
        CameraStream.isEnabled = false
        Drivetrain.setNeutralMode(NeutralMode.Brake)
        hasRunAuto = true
        Game.updateGameData()
        LEDController.alliance = Game.alliance
        Drivetrain.zeroGyro()
        AutoChooser.auto.launch()
    }

    override fun teleopInit() {
        CameraStream.isEnabled = true
        Drivetrain.setNeutralMode(NeutralMode.Brake)
        if (!hasRunAuto) runBlocking {
            Carriage.animateToPose(Pose.INTAKE)
        }
        LEDController.state = FireState
        Drivetrain.zeroEncoders()
        CommandSystem.initDefaultCommands()
    }

    override fun disabledInit() {
        CameraStream.isEnabled = true
        LEDController.state = IdleState
        Drivetrain.setNeutralMode(NeutralMode.Coast)
    }

    override fun disabledPeriodic() {
        if (SmartDashboard.getBoolean("Callibrate Gyro", false)) {
            Drivetrain.calibrateGyro()
            SmartDashboard.putBoolean("Callibrate Gyro", false)
        }
    }

    override fun testInit() {
        Drivetrain.calibrateGyro()
    }
}
