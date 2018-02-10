package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

class Robot : IterativeRobot() {
    override fun robotInit() {
        Drivetrain
        Driver
        Carriage
    }

    override fun robotPeriodic() {
        EventMapper.tick()
    }
}
