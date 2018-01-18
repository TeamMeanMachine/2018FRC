package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.IterativeRobot

class Robot : IterativeRobot() {
    lateinit var ultrasonic: AnalogInput

    override fun robotInit() {
        ultrasonic = AnalogInput(0)
    }

//    override fun disabledPeriodic() {
//        super.disabledPeriodic()
//        println("encoder: " + Arm.position)
//    }

    override fun teleopPeriodic() {
        print("Ultrasonic sensor: " + ultrasonic.value)
    }
}
