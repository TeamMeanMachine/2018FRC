package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation

object Game {
    val isEndGame: Boolean
        get() = DriverStation.getInstance().matchTime <= 30
}