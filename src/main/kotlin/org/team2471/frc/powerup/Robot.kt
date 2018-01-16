package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot

class Robot : IterativeRobot() {
    override fun robotInit() {
        Drive
        //Elevator
        Arm
        Intake
    }

//    override fun disabledPeriodic() {
//        super.disabledPeriodic()
//        println("encoder: " + Arm.position)
//    }

    override fun teleopPeriodic() {
        super.teleopPeriodic()
        println("encoder: " + Arm.position)
    }
}
