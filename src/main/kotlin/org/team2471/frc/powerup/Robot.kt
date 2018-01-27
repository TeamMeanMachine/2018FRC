package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import org.team2471.frc.lib.control.experimental.EventMapper

class Robot : IterativeRobot() {
    override fun robotInit() {
        Drive
        Arm
        Intake
        AutoChooser

        Driver
    }

    override fun autonomousInit() {
        AutoChooser.chosenAuto.launch()
    }

    override fun disabledPeriodic() {
//        println("Right: " + Drive.rightMotors.sensorCollection.quadraturePosition + ", Left: " + Drive.leftMotors.sensorCollection.quadraturePosition)
    }

    override fun teleopPeriodic() {
        EventMapper.tick()
    }

    override fun autonomousPeriodic() {
//        println("Left: " + Drive.leftMotors.sensorCollection.quadraturePosition + ", Right: " + Drive.rightMotors.sensorCollection.quadraturePosition)
//        println("Left Error: " + Drive.leftMotors.getClosedLoopError(0) + ", Right Error: " + Drive.rightMotors.getClosedLoopError(0))
        println(Drive.ticksToFeet(Drive.leftMotors.sensorCollection.quadraturePosition))
    }
}
