package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import org.team2471.frc.powerup.subsystems.dropOffToScale
import org.team2471.frc.powerup.subsystems.dropOffToScaleAuto

object AutoChooser {
    private val dashboard = SendableChooser<Command>().apply {
        addDefault(driveStraightAuto.name, driveStraightAuto)
        addObject(circleTest.name, circleTest)
        addObject(middleKillerAuto.name, middleKillerAuto)
        addObject(rightKillerAuto.name, rightKillerAuto)
        addObject(leftKillerAuto.name, leftKillerAuto)

        SmartDashboard.putData("Auto Chooser", this)
    }
    val chosenAuto: Command
        get() = dashboard.selected
}

val driveStraightAuto = Command("Drive Straight Auto", Drivetrain) {
    Drivetrain.driveDistance(10.0, 2.0)
}

val circleTest = Command("Circle Test Auto", Drivetrain) {
    Drivetrain.driveAlongPath(Path2D().apply {
        travelDirection = 1.0
        val tankDriveFudgeFactor = 1.097  // bigger factor will turn more, smaller less
        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor  // width in inches turned into feet
        val tangent = 6.0
        isMirrored = true

        addPointAndTangent(0.0, 0.0, 0.0, tangent)
        addPointAndTangent(4.0, 4.0, tangent, 0.0)
        addPointAndTangent(8.0, 0.0, 0.0, -tangent)
        addPointAndTangent(4.0, -4.0, -tangent, 0.0)
        addPointAndTangent(0.0, 0.0, 0.0, tangent)

        addEasePoint(0.0, 0.0)
        addEasePoint(16.0, 1.0)
    })
}

val middleKillerAuto = Command("Middle Killer Auto", Drivetrain, Carriage) {
    try {
        Drivetrain.driveAlongPath(centerToScale)
        dropOffToScale
        Carriage.Arm.playAnimation(Carriage.Arm.Animation.SCALE_TO_INTAKE)
        Drivetrain.driveAlongPath(fromScaleToSwitch)
        Carriage.Arm.intake = 1.0
        Carriage.Arm.clamp = true
        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
        Carriage.Arm.intake = -1.0
    } finally {
        Carriage.Arm.intake = 0.0
    }
}

val rightKillerAuto = Command("Right Killer Auto", Drivetrain, Carriage) {
    try {
        Drivetrain.driveAlongPath(rightToScale)
        dropOffToScaleAuto
        Drivetrain.driveAlongPath(fromScaleToSwitch)
        Carriage.Arm.clamp = true
        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
        Carriage.Arm.intake = -1.0
    } finally {
        Carriage.Arm.intake = 0.0
    }
}

val leftKillerAuto = Command("Left Killer Auto", Drivetrain, Carriage) {
    try {
        Drivetrain.driveAlongPath(leftToScale)
        dropOffToScaleAuto
        Drivetrain.driveAlongPath(fromScaleToSwitch)
        Carriage.Arm.clamp = true
        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
        Carriage.Arm.intake = -1.0
    } finally {
        Carriage.Arm.intake = 0.0
    }
}