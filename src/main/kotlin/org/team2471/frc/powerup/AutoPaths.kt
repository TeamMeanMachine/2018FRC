package org.team2471.frc.powerup

import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.motion_profiling.Path2D

val buildingPath = Command("Path Builder", Drive) {
    Drive.driveAlongPath(Path2D().apply {
        travelDirection = -1.0
        val tankDriveFudgeFactor = 1.097
        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor
    })

}