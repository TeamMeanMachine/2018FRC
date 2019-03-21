package org.team2471.frc.powerup.endgame

import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.I2C
import edu.wpi.first.wpilibj.Solenoid
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.Game
import org.team2471.frc.powerup.RobotMap

object Wings {
    private val wings = Solenoid(RobotMap.Solenoids.WINGS)
    private val climbingGuide = Solenoid(RobotMap.Solenoids.CLIMBING_GUIDE)
//    private val colorSensor = AdafruitColorSensor(I2C.Port.kOnboard, AdafruitColorSensor.IntegrationTime.T_2_4MS)

    init {
        if(!SmartDashboard.containsKey("Deploy Wings"))
            SmartDashboard.putBoolean("Deploy Wings", true)
        SmartDashboard.setPersistent("Deploy Wings")
        if(!SmartDashboard.containsKey("Solo Climb"))
            SmartDashboard.putBoolean("Solo Climb", false)
        SmartDashboard.setPersistent("Solo Climb")

        launch {
            val colorSensorTable = NetworkTableInstance.getDefault().getTable("Color Sensor")
            val clearEntry = colorSensorTable.getEntry("Clear")
            val redEntry = colorSensorTable.getEntry("Red")
            val greenEntry = colorSensorTable.getEntry("Green")
            val blueEntry = colorSensorTable.getEntry("Blue")

            val color = AdafruitColorSensor.Color(0, 0, 0, 0)
            periodic {
//                colorSensor.readColor(color)
                clearEntry.setNumber(color.clear)
                redEntry.setNumber(color.red)
                greenEntry.setNumber(color.green)
                blueEntry.setNumber(color.blue)
            }
        }
    }

    var wingsDeployed: Boolean
        get() = wings.get()
        set(value) = wings.set(value && Game.isEndGame)

    var climbingGuideDeployed: Boolean
        get() = climbingGuide.get()
        set(value) = climbingGuide.set(value)
}