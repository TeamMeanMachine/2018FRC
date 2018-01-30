package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import org.team2471.frc.lib.motion_profiling.Path2D

fun Path2D.setPathDefaults() {
    val tankDriveFudgeFactor = 1.097
    robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor
    println(gameData)
    isMirrored = gameData[1] == 'L'
}

val gameData get() = DriverStation.getInstance().gameSpecificMessage

val centerToScale = Path2D().apply {
    setPathDefaults()
    travelDirection = -1.0
    addPointAndTangent(0.0, 0.0, 0.0, -8.0)
    addPointAndTangent(-10.0, -11.5, 0.0, -16.0)
    addPointAndTangent(-7.25, -22.5, 0.0, -11.0)
    addEasePoint(0.0, 0.0)
    addEasePoint(5.0, 1.0)
}

val fromScaleToSwitch = Path2D().apply {
    setPathDefaults()
    if (gameData[0] == gameData[1]) {
        isMirrored = gameData[0] == 'R'
        travelDirection = 1.0
        addPointAndTangent(0.0, 0.0, 0.0, 4.0)
        addPointAndTangent(2.0, 5.0, 0.0, 4.0)
        addEasePoint(0.0, 0.0)
        addEasePoint(1.9, 1.0)
    } else {
        isMirrored = gameData[0] == 'L'
        travelDirection = 1.0
        addPointAndTangent(0.0, 0.0, 0.0, 4.0)
        addPointAndTangent(0.25, 3.0, 2.0, 0.0)
        addPointAndTangent(14.75, 3.0, 0.0, 3.0)
        addPointAndTangent(14.75, 5.5, 0.0, 5.0)
        addEasePoint(0.0, 0.0)
        addEasePoint(5.0, 1.0)
    }
}
