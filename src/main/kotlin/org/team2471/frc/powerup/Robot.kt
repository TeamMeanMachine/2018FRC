package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.RobotController
import edu.wpi.first.wpilibj.Solenoid

class Robot : IterativeRobot() {
    lateinit var ultrasonic: AnalogInput
    lateinit var solenoid: Solenoid

    override fun robotInit() {
        ultrasonic = AnalogInput(0)
        solenoid = Solenoid(3)
    }

    override fun teleopPeriodic() {
        val ultrasonicReading = ultrasonic.voltage / 0.2974848
        println(ultrasonicReading)
        solenoid.set(RobotController.getUserButton())
        // 4.88 mV per 5 mm , 4.88 * 60.96 = mV per foot , mV per foot = 297.4848 , V per foot = 0.2974848  MULTIPLY OR DIVIDE???
    }
}
