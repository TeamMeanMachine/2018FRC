package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import org.team2471.frc.lib.control.experimental.EventMapper
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

class Robot : IterativeRobot() {
    override fun robotInit() {
        Drivetrain
        Carriage
        Driver
    }

    override fun robotPeriodic() {
        EventMapper.tick()
        // println(Carriage.Arm.cubeSensor.voltage)
    }
}
