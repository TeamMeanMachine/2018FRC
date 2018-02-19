package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
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
            Carriage.Arm.stop()
            Carriage.Lifter.stop()
            dashboard.selected?.invoke(coroutineContext)
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
    delay(3500)
    Carriage.animateToPose(Carriage.Pose.SCALE_MED)
    Carriage.Arm.isClamping = false
    try {
        delay(200)
        launch(coroutineContext) {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }
        Drivetrain.driveAlongPath(autonomous.getPathOrCancel("RightScaleToCube1"))
    } finally {
        Carriage.Arm.isClamping = true
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
