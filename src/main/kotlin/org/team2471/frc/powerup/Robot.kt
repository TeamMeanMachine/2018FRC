package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

class Robot : IterativeRobot() {
    override fun robotInit() {
        Drivetrain
        Carriage
        Driver
        AutoChooser

        LiveWindow.disableAllTelemetry()
    }

    override fun autonomousInit() {
        AutoChooser.chosenAuto?.launch()
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        // println(Carriage.Arm.cubeSensor.voltage)
    }
}
