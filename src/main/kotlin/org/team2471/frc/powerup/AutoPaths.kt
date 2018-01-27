package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.motion_profiling.Path2D

val buildingPath = Command("Path Builder", Drive) {
    Drive.driveAlongPath(Path2D().apply {
        travelDirection = -1.0
        val tankDriveFudgeFactor = 1.097
        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor
        isMirrored = false
    })

}
val gameData = DriverStation.getInstance().gameSpecificMessage

val centerToScale = Command("Scale From Center", Drive){
    Drive.driveAlongPath(Path2D().apply {
        buildingPath
        if (gameData[1] == 'L') {
            isMirrored = false
        }
        if (gameData[1] == 'R') {
            isMirrored = true
        }

        travelDirection = -1.0
        addPointAndTangent(0.0, 0.0, 0.0, -2.0)
        addPointAndTangent(-10.0, 14.0, 0.0, 16.0)
        addPointAndTangent(-6.0, -24.5, 0.0, -28.0)
        addEasePoint(0.0, 0.0)
        addEasePoint(4.0, 1.0)


    })
}

val fromScaleToSwitch = Command("To Switch from Scale", Drive){
    Drive.driveAlongPath(Path2D().apply {
        buildingPath
        if (gameData[0] == 'L') {
            isMirrored = false
            travelDirection = 1.0
            addPointAndTangent(0.0, 0.0, 0.0, 4.0)
            addPointAndTangent(-0.5, 8.0, 0.0, 4.0)
            addEasePoint(0.0, 0.0)
            addEasePoint(2.0, 1.0)
        }
        if (gameData[0] == 'R') {
            isMirrored = false
            travelDirection = 1.0
            addPointAndTangent(0.0, 0.0, 0.0, 4.0)
            addPointAndTangent(13.5, 8.0, 0.0, 4.0)
            addEasePoint(0.0, 0.0)
            addEasePoint(3.0, 1.0)
        }
    })
}

val leftToScale = Command("Scale From Left", Drive){
    Drive.driveAlongPath(Path2D().apply {
        buildingPath
        if (gameData[1] == 'L'){
            isMirrored = false
            travelDirection = -1.0
            addPointAndTangent(0.0, 0.0, 0.0, -4.0)
        }
    })
}