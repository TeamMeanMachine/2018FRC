package org.team2471.frc.powerup.subsystems

import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.powerup.Game
import org.team2471.frc.powerup.RobotMap

object Wings {
    private val wings = Solenoid(RobotMap.Solenoids.WINGS)
    private val climbingGuide = Solenoid(RobotMap.Solenoids.CLIMBING_GUIDE)

    var wingsDeployed: Boolean
        get() = wings.get()
        set(value) = wings.set(value && Game.isEndGame)

    var climbingGuideDeployed: Boolean
        get() = climbingGuide.get()
        set(value) = climbingGuide.set(value)
}