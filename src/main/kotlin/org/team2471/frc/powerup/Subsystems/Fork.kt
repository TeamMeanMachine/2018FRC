package org.team2471.frc.powerup.Subsystems

import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.powerup.RobotMap

object Fork {
    private val left1 = Solenoid(RobotMap.Solenoids.FORK_LEFT_1)
    private val left2 = Solenoid(RobotMap.Solenoids.FORK_LEFT_2)
    private val right1 = Solenoid(RobotMap.Solenoids.FORK_RIGHT_1)
    private val right2 = Solenoid(RobotMap.Solenoids.FORK_RIGHT_2)

    fun deploy() {
        left1.set(true)
        left2.set(true)
        right1.set(true)
        right2.set(true)
    }
}