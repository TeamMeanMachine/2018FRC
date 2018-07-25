package org.team2471.frc.powerup.endgame

import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.powerup.Game
import org.team2471.frc.powerup.RobotMap

object Wings {
    private val wings = Solenoid(RobotMap.Solenoids.WINGS)
    private val climbingGuide = Solenoid(RobotMap.Solenoids.CLIMBING_GUIDE)

    init {
        if(!SmartDashboard.containsKey("Deploy Wings"))
            SmartDashboard.putBoolean("Deploy Wings", true)
        SmartDashboard.setPersistent("Deploy Wings")
        if(!SmartDashboard.containsKey("Solo Climb"))
            SmartDashboard.putBoolean("Solo Climb", false)
        SmartDashboard.setPersistent("Solo Climb")
    }

    var wingsDeployed: Boolean
        get() = wings.get()
        set(value) = wings.set(value && Game.isEndGame)

    var climbingGuideDeployed: Boolean
        get() = climbingGuide.get()
        set(value) = climbingGuide.set(value)
}