package org.team2471.frc.powerup


import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.motion_profiling.Path2D

object AutoChooser {
    private val dashboard = SendableChooser<Command>().apply {
        addDefault(driveStraightAuto.name, driveStraightAuto)
        addObject(circleTest.name, circleTest)
        addObject(centerToSwitch.name, centerToSwitch)

        SmartDashboard.putData("Auto Chooser", this)
    }
    val chosenAuto: Command
        get() = dashboard.selected
}


val driveStraightAuto = Command("Drive Straight Auto", Drive) {
    Drive.driveDistance(10.0, 2.0)
}

val circleTest = Command("Circle Test Auto", Drive) {
    Drive.driveAlongPath(Path2D().apply {
        travelDirection = 1.0
        val tankDriveFudgeFactor = 1.097  // bigger factor will turn more, smaller less
        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor  // width in inches turned into feet
        val tangent = 6.0
        isMirrored = false

        addPointAndTangent(0.0, 0.0, 0.0, tangent)
        addPointAndTangent(4.0, 4.0, tangent, 0.0)
        addPointAndTangent(8.0, 0.0, 0.0, -tangent)
        addPointAndTangent(4.0, -4.0, -tangent, 0.0)
        addPointAndTangent(0.0, 0.0, 0.0, tangent)

        addEasePoint(0.0, 0.0)
        addEasePoint(16.0, 1.0)
    })
}
val centerToSwitch = Command("Middle Killer Auto", Drive) {
    val gameData = DriverStation.getInstance().gameSpecificMessage
    Drive.driveAlongPath(Path2D().apply {
        travelDirection = -1.0
        val tankDriveFudgeFactor = 1.097
        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor
        isMirrored = false
        if (gameData[0] == 'L') {
            isMirrored = false
        }
        if (gameData[0] == 'R') {
            isMirrored = true
        }
            addPointAndTangent(0.0, 0.0, 0.0, -4.0)
            addPointAndTangent(-8.0, -5.0, -2.0, -2.0)
            addPointAndTangent(-8.0, -16.0, -5.0, -2.0)
            travelDirection = 1.0
            addPointAndTangent(-5.0, -14.0, -5.0, 4.0)
            addEasePoint(0.0, 0.0)
            addEasePoint(3.0, 1.0)

        if (gameData[1] == 'L') {
            isMirrored = false
        } // this is to go to the scale
        if (gameData[1] == 'R') {
            isMirrored = true
        }

            travelDirection = -1.0
            addPointAndTangent(-8.0, -26.0, -5.0, -28.0)
            addEasePoint(1.0, 1.0)

    })
}