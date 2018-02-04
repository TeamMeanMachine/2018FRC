package org.team2471.frc.powerup.subsystems

import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.powerup.Game
import org.team2471.frc.powerup.RobotMap

object Forks {
    private val leftSolenoid = Solenoid(RobotMap.Solenoids.FORK_LEFT)
    private val rightSolenoid = Solenoid(RobotMap.Solenoids.FORK_RIGHT)
    private val climbingGuide = Solenoid(RobotMap.Solenoids.CLIMBING_GUIDE)

    var leftSideDepolyed: Boolean
        get() = leftSolenoid.get()
        set(value) = leftSolenoid.set(value && Game.isEndGame)

    var rightSideDepolyed: Boolean
        get() = rightSolenoid.get()
        set(value) = rightSolenoid.set(value && Game.isEndGame)

    var climbingGuideDepolyed: Boolean
        get() = climbingGuide.get()
        set(value) = climbingGuide.set(value)
}