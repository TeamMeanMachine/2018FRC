package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.IterativeRobot

class Robot : IterativeRobot() {
    lateinit var beamBreak: AnalogInput

    override fun robotInit() {
        beamBreak = AnalogInput(0)
    }

    override fun robotPeriodic() {
        println(beamBreak.voltage)
    }
}
