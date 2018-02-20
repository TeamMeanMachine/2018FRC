package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain

private fun Autonomi.getAutoOrCancel(autoName: String) =
        this[autoName] ?: run {
            val message = "Failed to find auto $autoName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

private fun Autonomous.getPathOrCancel(pathName: String) =
        this[pathName] ?: run {
            val message = "Failed to find path $pathName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

object AutoChooser {
    private val dashboard = SendableChooser<Command>().apply {
        addDefault(driveStraightAuto.name, driveStraightAuto)
        addObject(circleTest.name, circleTest)
        addObject(rightScScSc.name, rightScScSc)
//        addObject(middleScalePlusSwitch.name, middleScalePlusSwitch)
//        addObject(rightScalePlusSwitch.name, rightScalePlusSwitch)
//        addObject(leftScalePlusSwitch.name, leftScalePlusSwitch)
//        addObject(middleSuperScale.name, middleSuperScale)
//        addObject(rightSuperScale.name, rightSuperScale)
//        addObject(leftSuperScale.name, leftSuperScale)
//        addDefault(armTestAuto.name, armTestAuto)

        SmartDashboard.putData("Auto Chooser", this)
    }
    val auto: Command
        get() = Command("Autonomous", Drivetrain, Carriage) {
            try {
                Carriage.Arm.stop()
                Carriage.Lifter.stop()
                RobotMap.compressor.closedLoopControl = false
                dashboard.selected?.invoke(coroutineContext)
            } finally {
                RobotMap.compressor.closedLoopControl = true
            }
        }
}

val driveStraightAuto = Command("Drive Straight Auto", Drivetrain) {
    launch(coroutineContext) {
        Carriage.animateToPose(Carriage.Pose.INTAKE)
    }
    Drivetrain.driveDistance(10.0, 4.0)
}

val rightScScSc = Command("RightScScSc", Drivetrain, Carriage) {
    val autonomous = autonomi.getAutoOrCancel("RightScScSc")

    launch(coroutineContext) {
        Drivetrain.driveAlongPath(autonomous.getPathOrCancel("RightToNearScale"))
    }
    delay(3000)
    Carriage.animateToPose(Carriage.Pose.SCALE_MED)
    Carriage.Arm.intake = -0.4
    try {
        delay(200)
        Carriage.Arm.isClamping = false
        var animate = launch(coroutineContext) {
            Carriage.animateToPose(Carriage.Pose.INTAKE, 1.3)
        }

        Carriage.Arm.intake = 0.7
        Drivetrain.driveAlongPath(autonomous.getPathOrCancel("RightScaleToCube1"))
        animate.cancelAndJoin()
        Carriage.Arm.isClamping = true
        delay(350)
        Carriage.Arm.intake = 0.2

        animate = launch(coroutineContext) {
            Carriage.animateToPose(Carriage.Pose.SCALE_LOW, 1.0)
        }

        Drivetrain.driveAlongPath(autonomous.getPathOrCancel("Cube1ToRightScale"))
        Carriage.Arm.intake = -0.2
        delay(200)
        animate.cancelAndJoin()
        Carriage.Arm.isClamping = false

        animate = launch(coroutineContext) {
            Carriage.animateToPose(Carriage.Pose.INTAKE, 0.7)
            Carriage.Arm.intake = 0.8
        }

        Drivetrain.driveAlongPath(autonomous.getPathOrCancel("RightScaleToCube2"))
        Carriage.Arm.isClamping = true
        animate.cancelAndJoin()
        delay(350)
        Carriage.Arm.intake = 0.2

        launch(coroutineContext) {
            Carriage.animateToPose(Carriage.Pose.SWITCH, 0.6)
        }
        Drivetrain.turnInPlace(35.0, 0.5)
        Drivetrain.driveDistance(0.5, 0.3, false)
        Carriage.Arm.intake = -0.4
        delay(1000)
    } finally {
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
    }
}

val circleTest = Command("Circle Test Auto", Drivetrain) {
    Drivetrain.driveAlongPath(circle)
}

val driveTuner = Command("Drive Train Tuner", Drivetrain) {
    Drivetrain.zeroDistance()
    periodic {
        Drivetrain.setDistance(Driver.throttle * 2.0)
    }
}
