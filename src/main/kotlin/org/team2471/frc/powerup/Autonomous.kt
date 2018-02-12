package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.commands.testLifter
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain


object AutoChooser {
    private val dashboard = SendableChooser<Command>().apply {
//        addDefault(driveStraightAuto.name, driveStraightAuto)
//        addObject(circleTest.name, circleTest)
//        addObject(middleScalePlusSwitch.name, middleScalePlusSwitch)
//        addObject(rightScalePlusSwitch.name, rightScalePlusSwitch)
//        addObject(leftScalePlusSwitch.name, leftScalePlusSwitch)
//        addObject(middleSuperScale.name, middleSuperScale)
//        addObject(rightSuperScale.name, rightSuperScale)
//        addObject(leftSuperScale.name, leftSuperScale)
        addDefault(armTestAuto.name, armTestAuto)

        SmartDashboard.putData("Auto Chooser", this)
    }
    val chosenAuto: Command?
        get() = dashboard.selected
}
//
//val driveStraightAuto = Command("Drive Straight Auto", Drivetrain) {
//    Drivetrain.driveDistance(10.0, 2.0)
//}
//
//val circleTest = Command("Circle Test Auto", Drivetrain) {
//    Drivetrain.driveAlongPath(Path2D().apply {
//        travelDirection = 1.0
//        val tankDriveFudgeFactor = 1.097  // bigger factor will turn more, smaller less
//        robotWidth = 35.0 / 12.0 * tankDriveFudgeFactor  // width in inches turned into feet
//        val tangent = 6.0
//        isMirrored = true
//
//        addPointAndTangent(0.0, 0.0, 0.0, tangent)
//        addPointAndTangent(4.0, 4.0, tangent, 0.0)
//        addPointAndTangent(8.0, 0.0, 0.0, -tangent)
//        addPointAndTangent(4.0, -4.0, -tangent, 0.0)
//        addPointAndTangent(0.0, 0.0, 0.0, tangent)
//
//        addEasePoint(0.0, 0.0)
//        addEasePoint(16.0, 1.0)
//    })
//}
//
//val middleScalePlusSwitch = Command("Middle Scale plus Switch Auto", Drivetrain, Carriage) {
//    try {
//        Drivetrain.driveAlongPath(centerToScale)
//        dropOffToScaleAuto
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.SCALE_TO_INTAKE)
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.intake = 1.0
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
//        Carriage.Arm.intake = -1.0
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}
//
//val rightScalePlusSwitch = Command("Right Scale plus Switch Auto", Drivetrain, Carriage) {
//    try {
//        Drivetrain.driveAlongPath(rightToScale)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
//        Carriage.Arm.intake = -1.0
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}
//
//val leftScalePlusSwitch = Command("Left Scale plus Switch Auto", Drivetrain, Carriage) {
//    try {
//        Drivetrain.driveAlongPath(leftToScale)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SWITCH)
//        Carriage.Arm.intake = -1.0
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}
////scale - switch - scale - scale - scale...
//val middleSuperScale = Command("Middle Super Scale Auto", Drivetrain, Carriage){
//    try {
//        Drivetrain.driveAlongPath(centerToScale)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromFirstCube)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(toSecondCube)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromSecondCube)
//        dropOffToScaleAuto
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}
//
//val rightSuperScale = Command("Right Super Scale Auto", Drivetrain, Carriage){
//    try {
//        Drivetrain.driveAlongPath(rightToScale)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromFirstCube)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(toSecondCube)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromSecondCube)
//        dropOffToScaleAuto
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}
//
//val leftSuperScale = Command("Left Super Scale Auto", Drivetrain, Carriage){
//    try {
//        Drivetrain.driveAlongPath(leftToScale)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(fromScaleToSwitch)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromFirstCube)
//        dropOffToScaleAuto
//        Drivetrain.driveAlongPath(toSecondCube)
//        Carriage.Arm.clamp = true
//        Carriage.Arm.playAnimation(Carriage.Arm.Animation.INTAKE_TO_SCALE)
//        Drivetrain.driveAlongPath(backFromSecondCube)
//        dropOffToScaleAuto
//    } finally {
//        Carriage.Arm.intake = 0.0
//    }
//}

val armTestAuto = Command("Testing Arm", Carriage){
    try {
        testLifter.invoke(coroutineContext)
        Carriage.Arm.intake = -1.0
        delay(300)
    } finally {
        Carriage.Arm.intake = 0.0
    }
}